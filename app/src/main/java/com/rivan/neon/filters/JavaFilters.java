package com.rivan.neon.filters;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;

/**
 * Utility class to apply various filters to a Bitmap using Java.
 *
 * @see com.rivan.neon.FilterProcessor
 */
public class JavaFilters {

    /**
     * Applies the Grayscale filter to a bitmap.
     *
     * @param bitmap           The bitmap to which the filter will be applied.
     * @param redCoefficient   Amount of Red value in a pixel. Defaults to 0.299.
     * @param greenCoefficient Amount of Green value in a pixel. Defaults to 0.587.
     * @param blueCoefficient  Amount of Blue value in a pixel. Defaults to 0.114.
     *
     * @return the same bitmap with the Grayscale filter applied.
     */
    public static Bitmap applyGrayscale(@NonNull Bitmap bitmap, float redCoefficient,
                                        float greenCoefficient, float blueCoefficient) {
        if (bitmap.isRecycled()) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;

        int[] pixels = new int[size];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < size; i++) {
            int pixel = pixels[i];

            int alpha = Color.alpha(pixel);
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);

            int gray = (int) (redCoefficient * red + greenCoefficient * green + blueCoefficient * blue);

            gray = clamp(gray);

            pixels[i] = Color.argb(alpha, gray, gray, gray);
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    /**
     * Applies the Invert filter to a bitmap.
     *
     * @param bitmap The bitmap to which the filter will be applied.
     *
     * @return the same bitmap with the Invert filter applied.
     */
    public static Bitmap applyInvert(@NonNull Bitmap bitmap) {
        if (bitmap.isRecycled()) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;

        int[] pixels = new int[size];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < size; i++) {
            int pixel = pixels[i];

            int alpha = Color.alpha(pixel);
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);

            int invertedRed = 255 - red;
            int invertedGreen = 255 - green;
            int invertedBlue = 255 - blue;

            pixels[i] = Color.argb(alpha, invertedRed, invertedGreen, invertedBlue);
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    /**
     * Applies the Brightness filter to a bitmap.
     *
     * @param bitmap     The bitmap to which the filter will be applied.
     * @param brightness Amount of brightness to be decreased or increased. Defaults to 0.
     *
     * @return the same bitmap with the Brightness filter applied.
     */
    public static Bitmap applyBrightness(@NonNull Bitmap bitmap, int brightness) {
        if (bitmap.isRecycled()) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;

        int[] pixels = new int[size];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < size; i++) {
            int pixel = pixels[i];

            int alpha = Color.alpha(pixel);
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);

            red += brightness;
            green += brightness;
            blue += brightness;

            red = clamp(red);
            green = clamp(green);
            blue = clamp(blue);

            pixels[i] = Color.argb(alpha, red, green, blue);
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    /**
     * Applies the Contrast filter to a bitmap.
     *
     * @param bitmap   The bitmap to which the filter will be applied.
     * @param contrast Amount of contrast to be decreased or increased. Defaults to 1.
     *
     * @return the same bitmap with the Contrast filter applied.
     */
    public static Bitmap applyContrast(@NonNull Bitmap bitmap, float contrast) {
        if (bitmap.isRecycled()) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;

        int[] pixels = new int[size];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < size; i++) {
            int pixel = pixels[i];

            int contrastedRed = (int) ((Color.red(pixel) - 128) * contrast + 128);
            int contrastedGreen = (int) ((Color.green(pixel) - 128) * contrast + 128);
            int contrastedBlue = (int) ((Color.blue(pixel) - 128) * contrast + 128);

            contrastedRed = clamp(contrastedRed);
            contrastedGreen = clamp(contrastedGreen);
            contrastedBlue = clamp(contrastedBlue);

            pixels[i] = Color.argb(Color.alpha(pixel), contrastedRed, contrastedGreen, contrastedBlue);
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    /**
     * Applies the Sepia filter to a bitmap.
     *
     * @param bitmap The bitmap to which the filter will be applied.
     *
     * @return the same bitmap with the Sepia filter applied.
     */
    public static Bitmap applySepia(@NonNull Bitmap bitmap) {
        if (bitmap.isRecycled()) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;

        int[] pixels = new int[size];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < size; i++) {
            int pixel = pixels[i];

            int alpha = Color.alpha(pixel);
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);

            double sepiaRed = (red * 0.393) + (green * 0.769) + (blue * 0.189);
            double sepiaGreen = (red * 0.349) + (green * 0.686) + (blue * 0.168);
            double sepiaBlue = (red * 0.272) + (green * 0.534) + (blue * 0.131);

            sepiaRed = clamp(sepiaRed);
            sepiaGreen = clamp(sepiaGreen);
            sepiaBlue = clamp(sepiaBlue);

            pixels[i] = Color.argb(alpha, (int) sepiaRed, (int) sepiaGreen, (int) sepiaBlue);
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(255, value));
    }
}
