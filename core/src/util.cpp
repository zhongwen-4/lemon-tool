#include "internal.h"

#include <cstdio>
#include <cstring>
#include <vector>

namespace mrs {

bool read_file(const std::string& path, std::string& out, size_t max_bytes) {
    FILE* file = fopen(path.c_str(), "rb");
    if (!file) return false;
    if (fseek(file, 0, SEEK_END) != 0) {
        fclose(file);
        return false;
    }
    long size = ftell(file);
    if (size < 0 || (size_t)size > max_bytes) {
        fclose(file);
        return false;
    }
    rewind(file);
    out.resize((size_t)size);
    size_t got = size ? fread(&out[0], 1, (size_t)size, file) : 0;
    fclose(file);
    if (got != (size_t)size) return false;
    return true;
}

std::string to_lower(std::string text) {
    for (size_t i = 0; i < text.size(); i++) {
        char c = text[i];
        if (c >= 'A' && c <= 'Z') text[i] = (char)(c - 'A' + 'a');
    }
    return text;
}

bool has_suffix(const std::string& text, const std::string& suffix) {
    if (text.size() < suffix.size()) return false;
    return text.compare(text.size() - suffix.size(), suffix.size(), suffix) == 0;
}

bool is_texty(const std::string& name) {
    static const char* kExt[] = {".sh",  ".prop", ".txt", ".xml", ".json", ".cfg",
                                 ".conf", ".ini", ".md",  ".rc",  ".te",  ".rule"};
    std::string lower = to_lower(name);
    for (size_t i = 0; i < sizeof(kExt) / sizeof(kExt[0]); i++) {
        if (has_suffix(lower, kExt[i])) return true;
    }
    return lower.find('.') == std::string::npos;
}

const char* elf_arch(const std::string& data) {
    if (data.size() < 20) return nullptr;
    const uint8_t* p = (const uint8_t*)data.data();
    if (p[0] != 0x7f || p[1] != 'E' || p[2] != 'L' || p[3] != 'F') return nullptr;
    uint16_t machine = (uint16_t)(p[18] | (p[19] << 8));
    switch (machine) {
        case 0x28: return "arm";
        case 0x3e: return "x86_64";
        case 0x03: return "x86";
        case 0xb7: return "arm64";
        default: return "未知架构";
    }
}

uint8_t elf_type(const std::string& data) {
    if (data.size() < 18) return 0;
    const uint8_t* p = (const uint8_t*)data.data();
    if (p[0] != 0x7f || p[1] != 'E' || p[2] != 'L' || p[3] != 'F') return 0;
    return (uint8_t)(p[16] | (p[17] << 8));
}

}
