#ifndef XXHASH64_H
#define XXHASH64_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

uint64_t xxhash64(const void* input, size_t length, uint64_t seed);

#ifdef __cplusplus
}
#endif

#endif
