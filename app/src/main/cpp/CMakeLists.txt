cmake_minimum_required(VERSION 3.22.1)

project("cleanify_hash" C)

add_library(
    cleanify_hash
    SHARED
    xxhash64.c
    native_hasher.c
)

target_include_directories(cleanify_hash PRIVATE .)

# Use well-defined behavior for pointer conversion (needed for xxHash unaligned reads)
target_compile_options(cleanify_hash PRIVATE -fno-strict-aliasing)
