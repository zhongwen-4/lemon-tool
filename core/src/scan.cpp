#include "internal.h"

#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#include <sys/stat.h>

#ifdef _WIN32
#include <io.h>
#else
#include <dirent.h>
#endif

namespace mrs {
namespace {

const size_t kMaxZipBytes = 64u * 1024u * 1024u;
const size_t kMaxEntryBytes = 8u * 1024u * 1024u;
const size_t kSniffBytes = 4096;
const size_t kMaxFindings = 500;
const size_t kBase64Run = 200;
const int kMaxDepth = 16;

bool is_regular_file(const std::string& path) {
    struct stat info;
    if (stat(path.c_str(), &info) != 0) return false;
#ifdef _WIN32
    return (info.st_mode & _S_IFMT) == _S_IFREG;
#else
    return S_ISREG(info.st_mode);
#endif
}

std::string trim(const std::string& text) {
    size_t begin = 0;
    size_t end = text.size();
    while (begin < end && (text[begin] == ' ' || text[begin] == '\t' || text[begin] == '\r')) begin++;
    while (end > begin && (text[end - 1] == ' ' || text[end - 1] == '\t' || text[end - 1] == '\r')) end--;
    return text.substr(begin, end - begin);
}

std::string basename(const std::string& path) {
    size_t pos = path.find_last_of('/');
    return pos == std::string::npos ? path : path.substr(pos + 1);
}

bool is_base64_run(const std::string& line) {
    size_t run = 0;
    for (size_t i = 0; i < line.size(); i++) {
        char c = line[i];
        bool member = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ||
                      c == '+' || c == '/' || c == '=';
        run = member ? run + 1 : 0;
        if (run >= kBase64Run) return true;
    }
    return false;
}

bool is_hook(const std::string& name) {
    static const char* kHooks[] = {"post-fs-data.sh", "service.sh", "customize.sh",
                                   "uninstall.sh", "boot-completed.sh"};
    std::string base = basename(name);
    for (size_t i = 0; i < sizeof(kHooks) / sizeof(kHooks[0]); i++)
        if (base == kHooks[i]) return true;
    return false;
}

bool is_script(const std::string& name) {
    return has_suffix(to_lower(name), ".sh") || is_hook(name);
}

// 卸载脚本只在模块被卸载时执行：它照扫、照进清单，但升级层不对它生效
bool is_uninstall_script(const std::string& name) {
    return basename(name) == "uninstall.sh";
}

void parse_prop(const std::string& text, ModuleInfo& info) {
    info.loaded = true;
    size_t start = 0;
    while (start <= text.size()) {
        size_t end = text.find('\n', start);
        if (end == std::string::npos) end = text.size();
        std::string line = trim(text.substr(start, end - start));
        size_t eq = line.find('=');
        if (eq != std::string::npos) {
            std::string key = trim(line.substr(0, eq));
            std::string value = trim(line.substr(eq + 1));
            if (key == "id") info.id = value;
            else if (key == "name") info.name = value;
            else if (key == "version") info.version = value;
            else if (key == "versionCode") info.versionCode = value;
            else if (key == "author") info.author = value;
            else if (key == "description") info.description = value;
        }
        if (end == text.size()) break;
        start = end + 1;
    }
}

struct Scanner {
    Report report;
    int system_files = 0;
    int elf_files = 0;
    int apk_files = 0;
    int setuid_files = 0;
    std::vector<std::string> elf_archs;
    std::vector<std::string> hooks;
    std::vector<std::string> uninstall_scripts;
    std::vector<std::string> urls;
    std::vector<std::string> ips;

    void add(Severity sev, const char* rule, const std::string& file, int line,
             const std::string& detail) {
        if (report.findings.size() >= kMaxFindings) return;
        Finding finding;
        finding.sev = sev;
        finding.rule = rule;
        finding.file = file;
        finding.line = line;
        finding.detail = detail;
        report.findings.push_back(finding);
    }

    void scan_text(const std::string& name, const std::string& text) {
        size_t rule_count = 0;
        const Rule* rules = rule_table(rule_count);
        size_t escalation_count = 0;
        const Escalation* escalations = escalation_table(escalation_count);
        bool script = is_script(name);
        bool uninstall = is_uninstall_script(name);
        bool saw_eval = false;
        bool saw_payload = false;
        int eval_line = 0;

        size_t start = 0;
        int line_no = 0;
        while (start <= text.size()) {
            size_t end = text.find('\n', start);
            if (end == std::string::npos) end = text.size();
            std::string line = text.substr(start, end - start);
            line_no++;
            for (size_t i = 0; i < rule_count; i++)
                if (line.find(rules[i].needle) != std::string::npos)
                    add(rules[i].sev, rules[i].id, name, line_no, rules[i].detail);
            if (!uninstall)
                for (size_t i = 0; i < escalation_count; i++)
                    if (escalations[i].match(line))
                        add(escalations[i].sev, escalations[i].id, name, line_no, escalations[i].detail);
            if (script && !uninstall && is_base64_run(line))
                add(Severity::Medium, "obf.blob", name, line_no,
                    "脚本里出现超长 base64 串，疑似混淆载荷");
            if (!saw_eval && line.find("eval ") != std::string::npos) {
                saw_eval = true;
                eval_line = line_no;
            }
            if (line.find("curl ") != std::string::npos ||
                line.find("wget ") != std::string::npos ||
                line.find("base64 -d") != std::string::npos)
                saw_payload = true;
            if (end == text.size()) break;
            start = end + 1;
        }
        if (saw_eval && saw_payload && !uninstall)
            add(Severity::High, "obf.eval-dynamic", name, eval_line,
                "同一文件里既有动态执行又有下载/解码，疑似远程载荷");
        extract_iocs(text, urls, ips);
    }

    void consider(const std::string& name, const std::string& content, bool text_mode,
                  bool has_mode, uint32_t mode) {
        report.entries.push_back(name);
        report.file_count++;
        if (name.compare(0, 7, "system/") == 0) system_files++;
        if (has_suffix(to_lower(name), ".apk")) apk_files++;
        if (is_hook(name)) hooks.push_back(name);
        if (is_uninstall_script(name)) uninstall_scripts.push_back(name);
        if (has_mode && (mode & 04000u)) {
            setuid_files++;
            add(Severity::High, "fs.setuid", name, 0, "条目带 setuid 位，安装后可能以特权身份执行");
        }
        if (basename(name) == "module.prop") {
            parse_prop(content, report.module);
            return;
        }
        if (text_mode) {
            scan_text(name, content);
            return;
        }
        const char* arch = elf_arch(content);
        if (arch) {
            elf_files++;
            bool known = false;
            for (size_t i = 0; i < elf_archs.size(); i++)
                if (elf_archs[i] == arch) known = true;
            if (!known) elf_archs.push_back(arch);
        }
    }

    void finish() {
        if (!report.module.loaded)
            add(Severity::Medium, "module.prop.missing", "module.prop", 0,
                "缺少 module.prop，可能不是标准模块包");
        for (size_t i = 0; i < urls.size(); i++)
            add(Severity::Low, "net.url", "", 0, "脚本引用外部地址：" + urls[i]);
        for (size_t i = 0; i < ips.size(); i++)
            add(Severity::Low, "net.ip", "", 0, "脚本出现 IP 地址：" + ips[i]);

        if (!hooks.empty()) {
            std::string joined;
            for (size_t i = 0; i < hooks.size(); i++)
                joined += (i ? ", " : "") + hooks[i];
            report.notes.push_back("开机/安装钩子：" + joined);
        }
        if (!uninstall_scripts.empty()) {
            std::string joined;
            for (size_t i = 0; i < uninstall_scripts.size(); i++)
                joined += (i ? ", " : "") + uninstall_scripts[i];
            report.notes.push_back("卸载脚本 " + joined +
                                   " 只在模块被卸载时执行，其中的行为只作记录、不计入风险统计");
        }
        if (system_files)
            report.notes.push_back("system/ 下 " + std::to_string(system_files) +
                                   " 个文件会覆盖系统分区内容");
        if (elf_files) {
            std::string arches;
            for (size_t i = 0; i < elf_archs.size(); i++)
                arches += (i ? "/" : "") + elf_archs[i];
            report.notes.push_back("内含 " + std::to_string(elf_files) + " 个可执行文件（" +
                                   arches + "）");
        }
        if (apk_files)
            report.notes.push_back("包含 " + std::to_string(apk_files) + " 个 APK 文件");
        if (setuid_files)
            report.notes.push_back("包含 " + std::to_string(setuid_files) + " 个 setuid 文件");
    }
};

#ifdef _WIN32

void walk_dir(const std::string& root, const std::string& relative, int depth, Scanner& scanner) {
    if (depth > kMaxDepth) return;
    std::string pattern = relative.empty() ? root + "\\*" : root + "\\" + relative + "\\*";
    for (size_t i = 0; i < pattern.size(); i++)
        if (pattern[i] == '/') pattern[i] = '\\';
    _finddata_t found;
    intptr_t handle = _findfirst(pattern.c_str(), &found);
    if (handle == -1) return;
    do {
        std::string name = found.name;
        if (name == "." || name == "..") continue;
        std::string child = relative.empty() ? name : relative + "/" + name;
        if (found.attrib & _A_SUBDIR) {
            walk_dir(root, child, depth + 1, scanner);
            continue;
        }
        std::string data;
        if (!read_file(root + "/" + child, data, kMaxEntryBytes)) continue;
        bool text_mode = is_texty(child);
        if (!text_mode && !elf_arch(data)) continue;
        scanner.consider(child, data, text_mode, false, 0);
    } while (_findnext(handle, &found) == 0);
    _findclose(handle);
}

#else

void walk_dir(const std::string& root, const std::string& relative, int depth, Scanner& scanner) {
    if (depth > kMaxDepth) return;
    std::string dir = relative.empty() ? root : root + "/" + relative;
    DIR* handle = opendir(dir.c_str());
    if (!handle) return;
    struct dirent* item = nullptr;
    while ((item = readdir(handle)) != nullptr) {
        std::string name = item->d_name;
        if (name == "." || name == "..") continue;
        std::string child = relative.empty() ? name : relative + "/" + name;
        std::string full = root + "/" + child;
        struct stat info;
        if (stat(full.c_str(), &info) != 0) continue;
        if (S_ISDIR(info.st_mode)) {
            walk_dir(root, child, depth + 1, scanner);
            continue;
        }
        if (!S_ISREG(info.st_mode)) continue;
        std::string data;
        if (!read_file(full, data, kMaxEntryBytes)) continue;
        bool text_mode = is_texty(child);
        if (!text_mode && !elf_arch(data)) continue;
        scanner.consider(child, data, text_mode, true, (uint32_t)info.st_mode);
    }
    closedir(handle);
}

#endif

}

Report scan_zip(const std::string& path, std::string* error) {
    Scanner scanner;
    scanner.report.target = path;

    std::string data;
    if (!read_file(path, data, kMaxZipBytes)) {
        *error = "读取失败或文件超过 64 MB 上限";
        return scanner.report;
    }
    std::vector<Entry> entries;
    if (!zip_list(data, entries, error)) return scanner.report;

    for (size_t i = 0; i < entries.size(); i++) {
        const Entry& entry = entries[i];
        if (entry.is_dir) continue;
        bool text_mode = is_texty(entry.name) || basename(entry.name) == "module.prop";
        size_t cap = text_mode ? kMaxEntryBytes : kSniffBytes;
        std::string content;
        bool truncated = false;
        if (!zip_read(data, entry, content, cap, &truncated)) {
            scanner.add(Severity::Low, "zip.unreadable", entry.name, 0,
                        "条目无法解压（加密或压缩方式不支持）");
            continue;
        }
        if (truncated && text_mode) scanner.report.truncated = true;
        scanner.consider(entry.name, content, text_mode, true, entry.mode);
    }
    scanner.finish();
    return scanner.report;
}

Report scan_dir(const std::string& path, std::string* error) {
    Scanner scanner;
    scanner.report.target = path;
    walk_dir(path, "", 0, scanner);
    if (scanner.report.file_count == 0) *error = "目录为空或无法读取";
    scanner.finish();
    return scanner.report;
}

Report scan_path(const std::string& path, std::string* error) {
    if (is_regular_file(path)) return scan_zip(path, error);
    return scan_dir(path, error);
}

}
