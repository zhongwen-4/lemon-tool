#include "internal.h"

#include <cstring>
#include <vector>

namespace mrs {
namespace {

uint16_t le16(const uint8_t* p) { return (uint16_t)(p[0] | (p[1] << 8)); }

uint32_t le32(const uint8_t* p) {
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

bool find_eocd(const std::string& data, size_t& offset) {
    if (data.size() < 22) return false;
    size_t max_back = data.size() < 66000 ? data.size() : 66000;
    for (size_t back = 22; back <= max_back; back++) {
        size_t pos = data.size() - back;
        if (le32((const uint8_t*)data.data() + pos) == 0x06054b50u) {
            offset = pos;
            return true;
        }
    }
    return false;
}

}

bool zip_list(const std::string& data, std::vector<Entry>& entries, std::string* error) {
    size_t eocd = 0;
    if (!find_eocd(data, eocd)) {
        *error = "找不到 ZIP 中央目录，可能不是 zip 文件";
        return false;
    }
    const uint8_t* base = (const uint8_t*)data.data();
    uint16_t count = le16(base + eocd + 10);
    uint32_t cd_size = le32(base + eocd + 12);
    uint32_t cd_off = le32(base + eocd + 16);
    (void)cd_size;

    if (count == 0xFFFF || cd_off == 0xFFFFFFFFu) {
        *error = "ZIP64 暂不支持";
        return false;
    }
    if ((size_t)cd_off >= data.size()) {
        *error = "ZIP 中央目录偏移越界";
        return false;
    }

    size_t pos = cd_off;
    for (uint16_t i = 0; i < count; i++) {
        if (pos + 46 > data.size() || le32(base + pos) != 0x02014b50u) {
            *error = "ZIP 中央目录条目损坏";
            return false;
        }
        Entry entry;
        entry.method = le16(base + pos + 10);
        entry.comp_size = le32(base + pos + 20);
        entry.raw_size = le32(base + pos + 24);
        uint16_t name_len = le16(base + pos + 28);
        uint16_t extra_len = le16(base + pos + 30);
        uint16_t comment_len = le16(base + pos + 32);
        entry.local_off = le32(base + pos + 42);
        uint32_t external = le32(base + pos + 38);
        entry.mode = external >> 16;
        if (pos + 46 + name_len > data.size()) {
            *error = "ZIP 条目名越界";
            return false;
        }
        entry.name.assign(data, pos + 46, name_len);
        entry.is_dir = !entry.name.empty() && entry.name[entry.name.size() - 1] == '/';
        entries.push_back(entry);
        pos += 46 + (size_t)name_len + extra_len + comment_len;
    }
    return true;
}

bool zip_read(const std::string& data, const Entry& entry, std::string& out, size_t max_bytes,
              bool* truncated) {
    *truncated = false;
    const uint8_t* base = (const uint8_t*)data.data();
    if ((size_t)entry.local_off + 30 > data.size()) return false;
    size_t pos = entry.local_off;
    if (le32(base + pos) != 0x04034b50u) return false;
    uint16_t name_len = le16(base + pos + 26);
    uint16_t extra_len = le16(base + pos + 28);
    size_t data_off = pos + 30 + (size_t)name_len + extra_len;
    if (data_off + entry.comp_size > data.size()) return false;

    if (entry.method == 0) {
        size_t take = entry.comp_size;
        if (take > max_bytes) {
            take = max_bytes;
            *truncated = true;
        }
        out.assign(data, data_off, take);
        return true;
    }
    if (entry.method == 8) {
        int result = inflate_raw(base + data_off, entry.comp_size, max_bytes, out);
        if (result < 0) return false;
        if (result == 1) *truncated = true;
        return true;
    }
    return false;
}

}
