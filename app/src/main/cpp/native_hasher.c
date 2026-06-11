#include <jni.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include "xxhash64.h"

static char* bytesToHex(const uint8_t* bytes, size_t len) {
    const char hex[] = "0123456789abcdef";
    char* out = (char*)malloc(len * 2 + 1);
    if (!out) return NULL;
    for (size_t i = 0; i < len; i++) {
        out[i * 2] = hex[(bytes[i] >> 4) & 0xF];
        out[i * 2 + 1] = hex[bytes[i] & 0xF];
    }
    out[len * 2] = '\0';
    return out;
}

JNIEXPORT jstring JNICALL
Java_com_cleanify_util_NativeHasher_nativeHashFile(JNIEnv* env, jclass clazz, jint fd) {
    int fildes = (int)fd;
    struct stat st;
    if (fstat(fildes, &st) != 0) {
        close(fildes);
        return NULL;
    }

    size_t size = (size_t)st.st_size;
    uint8_t* mapped = NULL;
    if (size > 0) {
        mapped = (uint8_t*)mmap(NULL, size, PROT_READ, MAP_PRIVATE, fildes, 0);
        if (mapped == MAP_FAILED) {
            close(fildes);
            return NULL;
        }
    }

    uint64_t hash = xxhash64(mapped, size, 0);

    if (mapped) munmap(mapped, size);
    close(fildes);

    char* hex = bytesToHex((const uint8_t*)&hash, 8);
    jstring result = (*env)->NewStringUTF(env, hex);
    free(hex);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_cleanify_util_NativeHasher_nativeHashFilePartial(JNIEnv* env, jclass clazz,
                                                      jint fd, jint bytesToRead) {
    int fildes = (int)fd;
    struct stat st;
    if (fstat(fildes, &st) != 0) {
        close(fildes);
        return NULL;
    }

    size_t readSize = (size_t)(st.st_size < bytesToRead ? st.st_size : bytesToRead);
    uint8_t* buf = (uint8_t*)malloc(readSize);
    if (!buf) {
        close(fildes);
        return NULL;
    }

    size_t total = 0;
    while (total < readSize) {
        ssize_t r = read(fildes, buf + total, readSize - total);
        if (r <= 0) break;
        total += (size_t)r;
    }

    uint64_t hash = xxhash64(buf, total, 0);
    free(buf);
    close(fildes);

    char* hex = bytesToHex((const uint8_t*)&hash, 8);
    jstring result = (*env)->NewStringUTF(env, hex);
    free(hex);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_cleanify_util_NativeHasher_hashBytes(JNIEnv* env, jclass clazz,
                                                jbyteArray data) {
    jsize len = (*env)->GetArrayLength(env, data);
    jbyte* bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!bytes) return NULL;

    uint64_t hash = xxhash64(bytes, (size_t)len, 0);

    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);

    char* hex = bytesToHex((const uint8_t*)&hash, 8);
    jstring result = (*env)->NewStringUTF(env, hex);
    free(hex);
    return result;
}
