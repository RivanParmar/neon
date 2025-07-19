#include <android/bitmap.h>
#include <android/log.h>
#include <chrono>
#include <jni.h>

#define LOG_TAG "neon"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// These functions are implemented in separate Assembly files (e.g., grayscale.s, invert.s, etc.)
// and perform the actual pixel manipulation. They are declared 'extern "C"' to ensure C linkage
// so the JNI functions can call them directly.

extern "C" void grayscale(
        uint8_t* pixels,
        uint32_t width,
        uint32_t height,
        uint32_t stride,
        float redCoefficient,
        float greenCoefficient,
        float blueCoefficient);

extern "C" void invert(
        uint8_t* pixels,
        uint32_t width,
        uint32_t height,
        uint32_t stride);

extern "C" void brightness(
        uint8_t* pixels,
        uint32_t width,
        uint32_t height,
        uint32_t stride,
        int32_t brightness);

extern "C" void contrast(
        uint8_t* pixels,
        uint32_t width,
        uint32_t height,
        uint32_t stride,
        float contrast);

extern "C" void sepia(
        uint8_t* pixels,
        uint32_t width,
        uint32_t height,
        uint32_t stride);

// These JNI (Java Native Interface) functions serve as the bridge between the Java/Kotlin
// layer and the native Assembly filter implementations.
//
// For 'apply' functions:
// 1. They retrieve the Android Bitmap's information and validate its format (RGBA_8888).
// 2. They lock the bitmap's pixels to obtain a direct pointer to the raw pixel data.
// 3. They call the corresponding external Assembly function to perform the image processing.
// 4. Finally, they unlock the bitmap's pixels, making the modified data available to Java.
//
// For 'measure' functions:
// These follow a similar process but also include high-resolution timing around the call
// to the Assembly filter function. They return the measured execution time in nanoseconds
// as a jlong, or a negative error code if bitmap access fails.

extern "C" JNIEXPORT void JNICALL
Java_com_rivan_neon_filters_NativeFilters_applyGrayscale(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap,
        jfloat redCoefficient,
        jfloat greenCoefficient,
        jfloat blueCoefficient) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return;
    }

    grayscale(
            reinterpret_cast<uint8_t*>(pixels),
            info.width,
            info.height,
            info.stride,
            redCoefficient,
            greenCoefficient,
            blueCoefficient);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_rivan_neon_filters_NativeFilters_measureGrayscale(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap,
        jfloat redCoefficient,
        jfloat greenCoefficient,
        jfloat blueCoefficient) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return -1;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return -2;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return -3;
    }

    auto start_time = std::chrono::high_resolution_clock::now();

    grayscale(
            reinterpret_cast<uint8_t*>(pixels),
            info.width,
            info.height,
            info.stride,
            redCoefficient,
            greenCoefficient,
            blueCoefficient);

    auto end_time = std::chrono::high_resolution_clock::now();

    AndroidBitmap_unlockPixels(env, bitmap);

    return std::chrono::duration_cast<std::chrono::nanoseconds>(end_time - start_time).count();
}

extern "C" JNIEXPORT void JNICALL
Java_com_rivan_neon_filters_NativeFilters_applyInvert(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return;
    }

    invert(
           reinterpret_cast<uint8_t*>(pixels),
           info.width,
           info.height,
           info.stride);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_rivan_neon_filters_NativeFilters_measureInvert(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return -1;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return -2;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return -3;
    }

    auto start_time = std::chrono::high_resolution_clock::now();

    invert(
            reinterpret_cast<uint8_t*>(pixels),
            info.width,
            info.height,
            info.stride);

    auto end_time = std::chrono::high_resolution_clock::now();

    AndroidBitmap_unlockPixels(env, bitmap);

    return std::chrono::duration_cast<std::chrono::nanoseconds>(end_time - start_time).count();
}

extern "C" JNIEXPORT void JNICALL
Java_com_rivan_neon_filters_NativeFilters_applyBrightness(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap,
        jint brightnessAdjustment) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return;
    }

    brightness(
           reinterpret_cast<uint8_t*>(pixels),
           info.width,
           info.height,
           info.stride,
           brightnessAdjustment);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_rivan_neon_filters_NativeFilters_measureBrightness(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap,
        jint brightnessAdjustment) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return -1;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return -2;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return -3;
    }

    auto start_time = std::chrono::high_resolution_clock::now();

    brightness(
            reinterpret_cast<uint8_t*>(pixels),
            info.width,
            info.height,
            info.stride,
            brightnessAdjustment);

    auto end_time = std::chrono::high_resolution_clock::now();

    AndroidBitmap_unlockPixels(env, bitmap);

    return std::chrono::duration_cast<std::chrono::nanoseconds>(end_time - start_time).count();
}

extern "C" JNIEXPORT void JNICALL
Java_com_rivan_neon_filters_NativeFilters_applyContrast(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap,
        jfloat contrastFactor) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return;
    }

    contrast(
            reinterpret_cast<uint8_t*>(pixels),
            info.width,
            info.height,
            info.stride,
            contrastFactor);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_rivan_neon_filters_NativeFilters_measureContrast(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap,
        jfloat contrastFactor) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return -1;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return -2;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return -3;
    }

    auto start_time = std::chrono::high_resolution_clock::now();

    contrast(
            reinterpret_cast<uint8_t*>(pixels),
            info.width,
            info.height,
            info.stride,
            contrastFactor);

    auto end_time = std::chrono::high_resolution_clock::now();

    AndroidBitmap_unlockPixels(env, bitmap);

    return std::chrono::duration_cast<std::chrono::nanoseconds>(end_time - start_time).count();
}

extern "C" JNIEXPORT void JNICALL
Java_com_rivan_neon_filters_NativeFilters_applySepia(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return;
    }

    sepia(
          reinterpret_cast<uint8_t*>(pixels),
          info.width,
          info.height,
          info.stride);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_rivan_neon_filters_NativeFilters_measureSepia(
        JNIEnv *env,
        jclass /* this */,
        jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmapInfo_getInfo() failed! error=%d", ret);
        return -1;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return -2;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed! error=%d", ret);
        return -3;
    }

    auto start_time = std::chrono::high_resolution_clock::now();

    sepia(
            reinterpret_cast<uint8_t*>(pixels),
            info.width,
            info.height,
            info.stride);

    auto end_time = std::chrono::high_resolution_clock::now();

    AndroidBitmap_unlockPixels(env, bitmap);

    return std::chrono::duration_cast<std::chrono::nanoseconds>(end_time - start_time).count();
}
