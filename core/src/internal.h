#pragma once

#include "mrs/core.h"

#include <cstdint>
#include <string>
#include <vector>

namespace mrs {

struct Entry {
    std::string name;
    uint16_t method = 0;
    uint32_t comp_size = 0;
    uint32_t raw_size = 0;
    uint32_t local_off = 0;
    uint32_t mode = 0;
    bool is_dir = false;
};

bool read_file(const std::string& path, std::string& out, size_t max_bytes);

bool zip_list(const std::string& data, std::vector<Entry>& entries, std::string* error);
bool zip_read(const std::string& data, const Entry& entry, std::string& out,
              size_t max_bytes, bool* truncated);

int inflate_raw(const uint8_t* in, size_t in_len, size_t max_out, std::string& out);

std::string to_lower(std::string text);
bool has_suffix(const std::string& text, const std::string& suffix);
bool is_texty(const std::string& name);

const char* elf_arch(const std::string& data);
uint8_t elf_type(const std::string& data);

struct Rule {
    const char* id;
    Severity sev;
    const char* needle;
    const char* detail;
};

const Rule* rule_table(size_t& count);

// 升级判定：只有「组合」或「路径敏感」这类明确情形才给高危。
// 单条特征命中由 rule_table 负责，最高只到中危。
struct Escalation {
    const char* id;
    Severity sev;
    const char* detail;
    bool (*match)(const std::string& line);
};

const Escalation* escalation_table(size_t& count);

void extract_iocs(const std::string& text, std::vector<std::string>& urls,
                  std::vector<std::string>& ips);

}
