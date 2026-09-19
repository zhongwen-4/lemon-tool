#include "internal.h"

#include <cstring>

namespace mrs {

namespace {

const Rule kRules[] = {
    {"cmd.rm-rf", Severity::High, "rm -rf", "递归强制删除，可能清空系统或数据目录"},
    {"cmd.rm-rf-root", Severity::High, "rm -rf /", "删除根路径", " \t;&|"},
    {"cmd.dd", Severity::High, "dd if=", "底层块设备写入，可能损坏分区"},
    {"cmd.curl-sh", Severity::High, "curl ", "运行时下载外部内容（远程代码执行风险）"},
    {"cmd.wget-sh", Severity::High, "wget ", "运行时下载外部内容（远程代码执行风险）"},
    {"obf.eval", Severity::High, "eval ", "动态执行字符串，常见于混淆载荷"},
    {"obf.base64", Severity::High, "base64 -d", "解码后执行，常见于混淆载荷"},
    {"obf.xxd", Severity::Medium, "xxd -r", "十六进制还原，常见于混淆载荷"},
    {"cmd.chmod777", Severity::Medium, "chmod 777", "权限放宽到所有人可写可执行"},
    {"cmd.su", Severity::Medium, "su -c", "以 root 身份执行命令"},
    {"cmd.setenforce", Severity::Medium, "setenforce", "修改 SELinux 强制模式"},
    {"cmd.resetprop", Severity::Medium, "resetprop", "绕过只读属性保护写入系统属性"},
    {"cmd.iptables", Severity::Medium, "iptables", "修改防火墙规则"},
    {"cmd.mount", Severity::Medium, "mount -o", "重新挂载分区，可能改动只读系统分区"},
    {"net.nvram", Severity::Medium, "nvram", "改动设备持久化参数"},
    {"net.hosts", Severity::Medium, "/etc/hosts", "可能篡改域名解析"},
    {"cmd.chmod755", Severity::Low, "chmod 755", "设置可执行权限"},
    {"cmd.setprop", Severity::Low, "setprop ", "修改系统属性"},
    {"cmd.busybox", Severity::Low, "busybox", "使用 busybox 工具集"},
    {"cmd.insmod", Severity::Medium, "insmod", "加载内核模块"},
};

bool is_url_char(char c) {
    if (c <= ' ') return false;
    switch (c) {
        case '"':
        case '\'':
        case '`':
        case '<':
        case '>':
        case ')':
        case '(':
        case ',':
        case ';':
        case '\\':
            return false;
        default:
            return true;
    }
}

bool push_unique(std::vector<std::string>& list, const std::string& value, size_t cap) {
    if (value.empty() || list.size() >= cap) return false;
    for (size_t i = 0; i < list.size(); i++)
        if (list[i] == value) return false;
    list.push_back(value);
    return true;
}

bool is_ip(const std::string& text, size_t start) {
    int octets = 0;
    size_t pos = start;
    while (octets < 4) {
        int digits = 0;
        int value = 0;
        while (pos < text.size() && text[pos] >= '0' && text[pos] <= '9') {
            value = value * 10 + (text[pos] - '0');
            digits++;
            pos++;
            if (digits > 3) return false;
        }
        if (digits == 0 || value > 255) return false;
        octets++;
        if (octets == 4) break;
        if (pos >= text.size() || text[pos] != '.') return false;
        pos++;
    }
    if (pos < text.size()) {
        char next = text[pos];
        if ((next >= '0' && next <= '9') || next == '.' || next == '-') return false;
    }
    return true;
}

}

const Rule* rule_table(size_t& count) {
    count = sizeof(kRules) / sizeof(kRules[0]);
    return kRules;
}

void extract_iocs(const std::string& text, std::vector<std::string>& urls,
                  std::vector<std::string>& ips) {
    for (size_t i = 0; i < text.size(); i++) {
        if (text.compare(i, 7, "http://") == 0 || text.compare(i, 8, "https://") == 0) {
            size_t end = i;
            while (end < text.size() && is_url_char(text[end])) end++;
            push_unique(urls, text.substr(i, end - i), 50);
            i = end;
            continue;
        }
        if (text[i] >= '0' && text[i] <= '9') {
            bool at_start = i == 0 || !((text[i - 1] >= '0' && text[i - 1] <= '9') || text[i - 1] == '.');
            if (at_start && is_ip(text, i)) {
                size_t end = i;
                while (end < text.size() && ((text[end] >= '0' && text[end] <= '9') || text[end] == '.'))
                    end++;
                push_unique(ips, text.substr(i, end - i), 50);
                i = end;
                continue;
            }
        }
    }
}

}
