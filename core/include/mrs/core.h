#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace mrs {

enum class Severity { Info, Low, Medium, High };

struct Finding {
    Severity sev = Severity::Info;
    std::string rule;
    std::string file;
    int line = 0;
    std::string detail;
};

struct ModuleInfo {
    bool loaded = false;
    std::string id;
    std::string name;
    std::string version;
    std::string versionCode;
    std::string author;
    std::string description;
};

struct Report {
    std::string target;
    ModuleInfo module;
    std::vector<std::string> entries;
    std::vector<Finding> findings;
    std::vector<std::string> notes;
    int file_count = 0;
    bool truncated = false;
};

Report scan_zip(const std::string& path, std::string* error);
Report scan_dir(const std::string& path, std::string* error);
Report scan_path(const std::string& path, std::string* error);

std::string to_text(const Report& report);
std::string to_json(const Report& report);
const char* severity_name(Severity sev);

}
