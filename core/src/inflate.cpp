#include "internal.h"

#include <cstring>
#include <vector>

namespace mrs {
namespace {

struct Bits {
    const uint8_t* data = nullptr;
    size_t len = 0;
    size_t pos = 0;
    uint32_t buf = 0;
    int cnt = 0;
    bool bad = false;

    int get(int need) {
        while (cnt < need) {
            if (pos >= len) {
                bad = true;
                return 0;
            }
            buf |= (uint32_t)data[pos++] << cnt;
            cnt += 8;
        }
        int value = (int)(buf & ((1u << need) - 1));
        buf >>= need;
        cnt -= need;
        return value;
    }

    void align() {
        int drop = cnt & 7;
        buf >>= drop;
        cnt -= drop;
    }
};

struct Huffman {
    short count[16];
    short symbol[288];
};

int build(Huffman& h, const uint8_t* lengths, int n) {
    for (int i = 0; i < 16; i++) h.count[i] = 0;
    for (int i = 0; i < n; i++) h.count[lengths[i]]++;
    if (h.count[0] == n) return 0;

    int left = 1;
    for (int len = 1; len < 16; len++) {
        left <<= 1;
        left -= h.count[len];
        if (left < 0) return -1;
    }

    short offs[16];
    offs[1] = 0;
    for (int len = 1; len < 15; len++) offs[len + 1] = (short)(offs[len] + h.count[len]);
    for (int i = 0; i < n; i++)
        if (lengths[i]) h.symbol[offs[lengths[i]]++] = (short)i;
    return left;
}

int decode(Bits& bits, const Huffman& h) {
    int code = 0;
    int first = 0;
    int index = 0;
    for (int len = 1; len < 16; len++) {
        code |= bits.get(1);
        if (bits.bad) return -1;
        int count = h.count[len];
        if (code - count < first) return h.symbol[index + (code - first)];
        index += count;
        first += count;
        first <<= 1;
        code <<= 1;
    }
    return -1;
}

const uint16_t kLenBase[29] = {3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
                               35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258};
const uint8_t kLenExtra[29] = {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
                               3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0};
const uint16_t kDistBase[30] = {1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
                                257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
                                8193, 12289, 16385, 24577};
const uint8_t kDistExtra[30] = {0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
                                7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13};
const uint8_t kLengthOrder[19] = {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};

enum class BlockResult { Ok, Truncated, Error };

BlockResult codes(Bits& bits, const Huffman& lit, const Huffman& dist, std::string& out,
                  size_t max_out, bool* capped) {
    for (;;) {
        int symbol = decode(bits, lit);
        if (symbol < 0) return BlockResult::Error;
        if (symbol < 256) {
            if (out.size() >= max_out) {
                *capped = true;
                return BlockResult::Truncated;
            }
            out.push_back((char)symbol);
            continue;
        }
        if (symbol == 256) return BlockResult::Ok;
        symbol -= 257;
        if (symbol >= 29) return BlockResult::Error;

        int length = kLenBase[symbol] + bits.get(kLenExtra[symbol]);
        int dsym = decode(bits, dist);
        if (dsym < 0 || dsym >= 30) return BlockResult::Error;
        int distance = kDistBase[dsym] + bits.get(kDistExtra[dsym]);
        if (bits.bad) return BlockResult::Error;
        if ((size_t)distance > out.size()) return BlockResult::Error;

        size_t from = out.size() - (size_t)distance;
        for (int i = 0; i < length; i++) {
            if (out.size() >= max_out) {
                *capped = true;
                return BlockResult::Truncated;
            }
            out.push_back(out[from + (size_t)i]);
        }
    }
}

BlockResult fixed_block(Bits& bits, std::string& out, size_t max_out, bool* capped) {
    uint8_t lengths[288];
    for (int i = 0; i < 144; i++) lengths[i] = 8;
    for (int i = 144; i < 256; i++) lengths[i] = 9;
    for (int i = 256; i < 280; i++) lengths[i] = 7;
    for (int i = 280; i < 288; i++) lengths[i] = 8;
    Huffman lit, dist;
    if (build(lit, lengths, 288) < 0) return BlockResult::Error;
    uint8_t dlengths[30];
    for (int i = 0; i < 30; i++) dlengths[i] = 5;
    if (build(dist, dlengths, 30) < 0) return BlockResult::Error;
    return codes(bits, lit, dist, out, max_out, capped);
}

BlockResult dynamic_block(Bits& bits, std::string& out, size_t max_out, bool* capped) {
    int nlen = bits.get(5) + 257;
    int ndist = bits.get(5) + 1;
    int ncode = bits.get(4) + 4;
    if (bits.bad) return BlockResult::Error;
    if (nlen > 286 || ndist > 30) return BlockResult::Error;

    uint8_t lengths[320];
    memset(lengths, 0, sizeof(lengths));
    for (int i = 0; i < ncode; i++) lengths[kLengthOrder[i]] = (uint8_t)bits.get(3);
    if (bits.bad) return BlockResult::Error;

    Huffman lencode;
    if (build(lencode, lengths, 19) < 0) return BlockResult::Error;

    int index = 0;
    while (index < nlen + ndist) {
        int symbol = decode(bits, lencode);
        if (symbol < 0) return BlockResult::Error;
        if (symbol < 16) {
            lengths[index++] = (uint8_t)symbol;
            continue;
        }
        int repeat = 0;
        uint8_t value = 0;
        if (symbol == 16) {
            if (index == 0) return BlockResult::Error;
            value = lengths[index - 1];
            repeat = 3 + bits.get(2);
        } else if (symbol == 17) {
            repeat = 3 + bits.get(3);
        } else {
            repeat = 11 + bits.get(7);
        }
        if (bits.bad || index + repeat > nlen + ndist) return BlockResult::Error;
        while (repeat--) lengths[index++] = value;
    }

    Huffman lit, dist;
    if (build(lit, lengths, nlen) < 0) return BlockResult::Error;
    if (build(dist, lengths + nlen, ndist) < 0) return BlockResult::Error;
    return codes(bits, lit, dist, out, max_out, capped);
}

}

int inflate_raw(const uint8_t* in, size_t in_len, size_t max_out, std::string& out) {
    Bits bits;
    bits.data = in;
    bits.len = in_len;
    bool capped = false;

    for (;;) {
        int last = bits.get(1);
        int type = bits.get(2);
        if (bits.bad) return -1;

        BlockResult result = BlockResult::Error;
        if (type == 0) {
            bits.align();
            if (bits.pos + 4 > bits.len) return -1;
            int len = bits.data[bits.pos] | (bits.data[bits.pos + 1] << 8);
            bits.pos += 4;
            if (bits.pos + (size_t)len > bits.len) return -1;
            for (int i = 0; i < len; i++) {
                if (out.size() >= max_out) {
                    capped = true;
                    break;
                }
                out.push_back((char)bits.data[bits.pos + i]);
            }
            bits.pos += (size_t)len;
            bits.buf = 0;
            bits.cnt = 0;
            result = capped ? BlockResult::Truncated : BlockResult::Ok;
        } else if (type == 1) {
            result = fixed_block(bits, out, max_out, &capped);
        } else if (type == 2) {
            result = dynamic_block(bits, out, max_out, &capped);
        } else {
            return -1;
        }

        if (result == BlockResult::Error) return -1;
        if (result == BlockResult::Truncated) return 1;
        if (last) return 0;
    }
}

}
