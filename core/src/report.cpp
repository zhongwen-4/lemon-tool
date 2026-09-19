#include "internal.h"

#include <algorithm>
#include <cstdio>
#include <vector>

namespace mrs {
namespace {

std::string json_escape(const std::string& text) {
    std::string out;
    for (size_t i = 0; i < text.size(); i++) {
        unsigned char c = (unsigned char)text[i];
        switch (c) {
            case '"': out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (c < 0x20) {
                    char buf[8];
                    snprintf(buf, sizeof(buf), "\\u%04x", c);
                    out += buf;
                } else {
                    out.push_back((char)c);
                }
        }
    }
    return out;
}

const char* severity_key(Severity sev) {
    switch (sev) {
        case Severity::High: return "high";
        case Severity::Medium: return "medium";
        case Severity::Low: return "low";
        default: return "info";
    }
}

int rank(Severity sev) {
    switch (sev) {
        case Severity::High: return 0;
        case Severity::Medium: return 1;
        case Severity::Low: return 2;
        default: return 3;
    }
}

std::string verdict_text(const int counts[4]) {
    if (counts[0] > 0)
        return "发现 " + std::to_string(counts[0]) +
               " 项高危行为，建议不要安装；确需安装请先逐条核对高危项。";
    if (counts[1] > 0)
        return "未发现高危行为，有 " + std::to_string(counts[1]) +
               " 项需要留意（中危）；低危与信息只是模块的行为记录。";
    return "未发现高危或中危行为，其余条目只是模块的正常行为记录。";
}

}

const char* severity_name(Severity sev) {
    switch (sev) {
        case Severity::High: return "高危";
        case Severity::Medium: return "中危";
        case Severity::Low: return "低危";
        default: return "信息";
    }
}

std::string to_text(const Report& report) {
    std::string out;
    int counts[4] = {0, 0, 0, 0};
    for (size_t i = 0; i < report.findings.size(); i++) counts[rank(report.findings[i].sev)]++;

    out += "模块：" + (report.module.name.empty() ? std::string("(未提供)") : report.module.name);
    out += report.module.id.empty() ? "" : "（" + report.module.id + "）";
    out += "\n";
    if (!report.module.version.empty())
        out += "版本：" + report.module.version +
               (report.module.versionCode.empty() ? "" : "（versionCode " + report.module.versionCode + "）") + "\n";
    if (!report.module.author.empty()) out += "作者：" + report.module.author + "\n";
    out += "扫描对象：" + report.target + "\n";
    out += "文件数：" + std::to_string(report.file_count) + "\n";
    out += "风险统计：高危 " + std::to_string(counts[0]) + " · 中危 " + std::to_string(counts[1]) +
           " · 低危 " + std::to_string(counts[2]) + " · 信息 " + std::to_string(counts[3]) + "\n";
    out += "结论：" + verdict_text(counts) + "\n";
    if (report.truncated) out += "注意：部分条目过大，只扫描了前一段内容\n";
    out += "\n";

    std::vector<size_t> order(report.findings.size());
    for (size_t i = 0; i < order.size(); i++) order[i] = i;
    std::stable_sort(order.begin(), order.end(), [&report](size_t a, size_t b) {
        int ra = rank(report.findings[a].sev);
        int rb = rank(report.findings[b].sev);
        if (ra != rb) return ra < rb;
        return report.findings[a].rule < report.findings[b].rule;
    });

    if (order.empty()) {
        out += "未发现风险特征。\n";
    }
    for (size_t i = 0; i < order.size(); i++) {
        const Finding& finding = report.findings[order[i]];
        out += "[";
        out += severity_name(finding.sev);
        out += "] ";
        out += finding.rule;
        if (!finding.file.empty()) {
            out += "  " + finding.file;
            if (finding.line > 0) out += ":" + std::to_string(finding.line);
        }
        out += "\n    " + finding.detail + "\n";
    }

    if (!report.notes.empty()) {
        out += "\n提示：\n";
        for (size_t i = 0; i < report.notes.size(); i++) out += "- " + report.notes[i] + "\n";
    }
    return out;
}

std::string to_json(const Report& report) {
    int counts[4] = {0, 0, 0, 0};
    for (size_t i = 0; i < report.findings.size(); i++) counts[rank(report.findings[i].sev)]++;

    std::string out = "{";
    out += "\"target\":\"" + json_escape(report.target) + "\",";
    out += "\"truncated\":" + std::string(report.truncated ? "true" : "false") + ",";
    out += "\"fileCount\":" + std::to_string(report.file_count) + ",";
    out += "\"module\":{";
    out += "\"loaded\":" + std::string(report.module.loaded ? "true" : "false") + ",";
    out += "\"id\":\"" + json_escape(report.module.id) + "\",";
    out += "\"name\":\"" + json_escape(report.module.name) + "\",";
    out += "\"version\":\"" + json_escape(report.module.version) + "\",";
    out += "\"versionCode\":\"" + json_escape(report.module.versionCode) + "\",";
    out += "\"author\":\"" + json_escape(report.module.author) + "\",";
    out += "\"description\":\"" + json_escape(report.module.description) + "\"},";
    out += "\"counts\":{\"high\":" + std::to_string(counts[0]) +
           ",\"medium\":" + std::to_string(counts[1]) + ",\"low\":" + std::to_string(counts[2]) +
           ",\"info\":" + std::to_string(counts[3]) + "},";
    out += "\"verdict\":\"" + json_escape(verdict_text(counts)) + "\",";
    out += "\"findings\":[";
    for (size_t i = 0; i < report.findings.size(); i++) {
        const Finding& finding = report.findings[i];
        if (i) out += ",";
        out += "{\"severity\":\"" + std::string(severity_key(finding.sev)) + "\",";
        out += "\"rule\":\"" + json_escape(finding.rule) + "\",";
        out += "\"file\":\"" + json_escape(finding.file) + "\",";
        out += "\"line\":" + std::to_string(finding.line) + ",";
        out += "\"detail\":\"" + json_escape(finding.detail) + "\"}";
    }
    out += "],\"notes\":[";
    for (size_t i = 0; i < report.notes.size(); i++) {
        if (i) out += ",";
        out += "\"" + json_escape(report.notes[i]) + "\"";
    }
    out += "]}";
    return out;
}

}
