#include "mrs/core.h"

#include <cstdio>
#include <cstring>
#include <string>

int main(int argc, char** argv) {
    bool json = false;
    std::string path;
    for (int i = 1; i < argc; i++) {
        std::string arg = argv[i];
        if (arg == "--json") {
            json = true;
        } else if (path.empty()) {
            path = arg;
        }
    }
    if (path.empty()) {
        fprintf(stderr, "用法: mrs <模块.zip|目录> [--json]\n");
        return 1;
    }

    std::string error;
    mrs::Report report;
    FILE* probe = fopen(path.c_str(), "rb");
    bool is_file = probe != nullptr;
    if (probe) fclose(probe);

    if (is_file) {
        report = mrs::scan_zip(path, &error);
    } else {
        report = mrs::scan_dir(path, &error);
    }

    if (!error.empty()) {
        fprintf(stderr, "扫描失败: %s\n", error.c_str());
        return 1;
    }

    if (json) {
        printf("%s\n", mrs::to_json(report).c_str());
    } else {
        printf("%s", mrs::to_text(report).c_str());
    }
    return 0;
}
