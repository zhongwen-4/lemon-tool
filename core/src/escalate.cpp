#include "internal.h"

#include <cstring>

namespace mrs {
namespace {

bool has(const std::string& line, const char* needle) {
    return line.find(needle) != std::string::npos;
}

std::vector<std::string> split_args(const std::string& text) {
    std::vector<std::string> parts;
    std::string current;
    for (size_t i = 0; i < text.size(); i++) {
        char c = text[i];
        bool separator = c == ' ' || c == '\t' || c == '"' || c == '\'' || c == ';' ||
                         c == '&' || c == '|';
        if (separator) {
            if (!current.empty()) parts.push_back(current);
            current.clear();
            continue;
        }
        current.push_back(c);
    }
    if (!current.empty()) parts.push_back(current);
    return parts;
}

bool under(const std::string& path, const char* root) {
    size_t len = strlen(root);
    if (path.size() < len || path.compare(0, len, root) != 0) return false;
    return path.size() == len || path[len] == '/';
}

// 删除/改写路径的风险档位
enum class Risk { None, Module, Critical };

Risk path_risk(const std::string& path) {
    if (path.empty() || path[0] != '/') return Risk::None;  // 相对路径（含 $MODDIR/*）不判敏感
    static const char* kScratch[] = {"/data/local/tmp", "/sdcard", "/storage", "/mnt",
                                     "/data/media"};
    for (size_t i = 0; i < sizeof(kScratch) / sizeof(kScratch[0]); i++)
        if (under(path, kScratch[i])) return Risk::None;
    if (path == "/" || path == "/*") return Risk::Critical;
    if (under(path, "/data/adb")) return Risk::Module;  // 模块数据库：破坏性大，但常是自删
    static const char* kCritical[] = {"/system", "/vendor", "/product", "/odm", "/apex",
                                      "/sbin", "/bin", "/etc", "/boot", "/init", "/data"};
    for (size_t i = 0; i < sizeof(kCritical) / sizeof(kCritical[0]); i++)
        if (under(path, kCritical[i])) return Risk::Critical;
    return Risk::None;
}

Risk worst_arg_risk(const std::vector<std::string>& args) {
    Risk worst = Risk::None;
    for (size_t i = 0; i < args.size(); i++) {
        if (args[i][0] == '-') continue;
        Risk risk = path_risk(args[i]);
        if (risk == Risk::Critical) return Risk::Critical;
        if (risk == Risk::Module) worst = Risk::Module;
    }
    return worst;
}

bool recursive_rm(const std::vector<std::string>& args) {
    for (size_t i = 0; i < args.size(); i++) {
        if (args[i].size() < 2 || args[i][0] != '-') continue;
        if (args[i].find('r') != std::string::npos || args[i].find('R') != std::string::npos ||
            args[i] == "--recursive")
            return true;
    }
    return false;
}

// 0 = 不敏感，1 = /data/adb 下的模块数据，2 = 设备关键路径
int rm_class(const std::string& line) {
    size_t pos = line.find("rm ");
    if (pos == std::string::npos) return 0;
    std::vector<std::string> args = split_args(line.substr(pos));
    if (!recursive_rm(args)) return 0;
    Risk risk = worst_arg_risk(args);
    if (risk == Risk::Critical) return 2;
    return risk == Risk::Module ? 1 : 0;
}

bool rm_device(const std::string& line) {
    return rm_class(line) == 2;
}

bool rm_module_data(const std::string& line) {
    return rm_class(line) == 1;
}

bool pipes_to_shell(const std::string& line) {
    static const char* kPipes[] = {"| sh",  "|sh",   "| bash", "|bash",
                                   "|su",   "| su",  "|sh -"};  // 管道给 shell 才算「下载即执行」
    for (size_t i = 0; i < sizeof(kPipes) / sizeof(kPipes[0]); i++)
        if (has(line, kPipes[i])) return true;
    return false;
}

bool download_to_shell(const std::string& line) {
    return (has(line, "curl") || has(line, "wget")) && pipes_to_shell(line);
}

bool decode_to_shell(const std::string& line) {
    return (has(line, "base64 -d") || has(line, "xxd -r")) && pipes_to_shell(line);
}

bool dd_block_device(const std::string& line) {
    return has(line, "dd ") && (has(line, "of=/dev/block") || has(line, "of=/dev/"));
}

bool resetprop_readonly(const std::string& line) {
    if (!has(line, "resetprop")) return false;
    std::vector<std::string> args = split_args(line);
    for (size_t i = 0; i < args.size(); i++)
        if (args[i].compare(0, 3, "ro.") == 0) return true;
    return false;
}

bool arg_is_zero(const std::string& line) {
    std::vector<std::string> args = split_args(line);
    for (size_t i = 0; i < args.size(); i++)
        if (args[i] == "0") return true;
    return false;
}

bool setenforce_off(const std::string& line) {
    return has(line, "setenforce") && arg_is_zero(line);
}

bool chmod777_critical(const std::string& line) {
    return has(line, "chmod 777") && worst_arg_risk(split_args(line)) == Risk::Critical;
}

bool remount_critical(const std::string& line) {
    return has(line, "remount") && worst_arg_risk(split_args(line)) == Risk::Critical;
}

const Escalation kEscalations[] = {
    {"cmd.rm-rf-device", Severity::High,
     "递归删除 /、/system、/data 等设备关键路径，可能让设备起不来", rm_device},
    {"net.exec-download", Severity::High, "把下载到的内容直接管道进 shell 执行", download_to_shell},
    {"obf.exec-decode", Severity::High, "把解码结果直接管道进 shell 执行，典型载荷投放",
     decode_to_shell},
    {"cmd.dd-block", Severity::High, "直接写块设备，误写会损坏分区", dd_block_device},
    {"cmd.resetprop-ro", Severity::High, "改写只读属性，常见于隐藏 root 与持久化",
     resetprop_readonly},
    {"cmd.rm-rf-adb", Severity::Medium, "递归删除 /data/adb 下的模块数据", rm_module_data},
    {"cmd.setenforce-off", Severity::Medium, "关闭 SELinux 强制模式", setenforce_off},
    {"cmd.chmod777-system", Severity::Medium, "把设备关键路径权限放宽到 777", chmod777_critical},
    {"cmd.remount-rw", Severity::Medium, "以可写方式重挂载设备分区", remount_critical},
};

}

const Escalation* escalation_table(size_t& count) {
    count = sizeof(kEscalations) / sizeof(kEscalations[0]);
    return kEscalations;
}

}
