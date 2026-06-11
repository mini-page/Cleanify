#include "xxhash64.h"

#define PRIME64_1 0x9E3779B185EBCA87ULL
#define PRIME64_2 0xC2B2AE3D27D4EB4FULL
#define PRIME64_3 0x165667B19E3779F9ULL
#define PRIME64_4 0x85EBCA77C2B2AE63ULL
#define PRIME64_5 0x27D4EB2F165667C5ULL

static inline uint64_t rotl64(uint64_t x, int r) {
    return (x << r) | (x >> (64 - r));
}

static inline uint64_t round64(uint64_t acc, uint64_t input) {
    acc += input * PRIME64_2;
    acc = rotl64(acc, 31);
    acc *= PRIME64_1;
    return acc;
}

static inline uint64_t mergeRound(uint64_t acc, uint64_t val) {
    val = round64(0, val);
    acc ^= val;
    acc = acc * PRIME64_1 + PRIME64_4;
    return acc;
}

static inline uint64_t avalanche(uint64_t h) {
    h ^= h >> 33;
    h *= PRIME64_2;
    h ^= h >> 29;
    h *= PRIME64_3;
    h ^= h >> 32;
    return h;
}

uint64_t xxhash64(const void* input, size_t length, uint64_t seed) {
    const uint8_t* p = (const uint8_t*)input;
    const uint8_t* const bEnd = p + length;
    uint64_t h64;

    if (length >= 32) {
        const uint8_t* const limit = bEnd - 32;
        uint64_t v1 = seed + PRIME64_1 + PRIME64_2;
        uint64_t v2 = seed + PRIME64_2;
        uint64_t v3 = seed;
        uint64_t v4 = seed - PRIME64_1;

        do {
            v1 = round64(v1, *(const uint64_t*)(p));
            v2 = round64(v2, *(const uint64_t*)(p + 8));
            v3 = round64(v3, *(const uint64_t*)(p + 16));
            v4 = round64(v4, *(const uint64_t*)(p + 24));
            p += 32;
        } while (p <= limit);

        h64 = rotl64(v1, 1) + rotl64(v2, 7) + rotl64(v3, 12) + rotl64(v4, 18);
        h64 = mergeRound(h64, v1);
        h64 = mergeRound(h64, v2);
        h64 = mergeRound(h64, v3);
        h64 = mergeRound(h64, v4);
    } else {
        h64 = seed + PRIME64_5;
    }

    h64 += (uint64_t)length;

    // Process remaining bytes (0-31)
    const uint8_t* end = bEnd;
    while (p + 8 <= end) {
        uint64_t k1 = *(const uint64_t*)(p);
        k1 *= PRIME64_2;
        k1 = rotl64(k1, 31);
        k1 *= PRIME64_1;
        h64 ^= k1;
        h64 = rotl64(h64, 27) * PRIME64_1 + PRIME64_4;
        p += 8;
    }
    if (p + 4 <= end) {
        uint64_t k1 = *(const uint32_t*)(p);
        k1 *= PRIME64_1;
        k1 = rotl64(k1, 23);
        k1 *= PRIME64_2;
        h64 ^= k1;
        h64 = rotl64(h64, 11) * PRIME64_1 + PRIME64_4;
        p += 4;
    }
    while (p < end) {
        uint64_t k1 = (*p) * PRIME64_5;
        k1 = rotl64(k1, 11);
        k1 *= PRIME64_1;
        h64 ^= k1;
        h64 = rotl64(h64, 11) * PRIME64_1 + PRIME64_4;
        p++;
    }

    return avalanche(h64);
}
