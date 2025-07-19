package com.rivan.neon.filters;

import android.graphics.Bitmap;

/**
 * Utility class that defines the JNI functions related to applying filters and
 * measuring their application time.
 */
public class NativeFilters {

    public static native void applyGrayscale(Bitmap bitmap, float redCoefficient,
                                             float greenCoefficient, float blueCoefficient);

    public static native long measureGrayscale(Bitmap bitmap, float redCoefficient,
                                               float greenCoefficient, float blueCoefficient);

    public static native void applyInvert(Bitmap bitmap);

    public static native long measureInvert(Bitmap bitmap);

    public static native void applyBrightness(Bitmap bitmap, int brightness);

    public static native long measureBrightness(Bitmap bitmap, int brightness);

    public static native void applyContrast(Bitmap bitmap, float contrast);

    public static native long measureContrast(Bitmap bitmap, float contrast);

    public static native void applySepia(Bitmap bitmap);

    public static native long measureSepia(Bitmap bitmap);
}
