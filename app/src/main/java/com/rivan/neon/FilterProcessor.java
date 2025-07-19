package com.rivan.neon;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rivan.neon.filters.JavaFilters;
import com.rivan.neon.filters.NativeFilters;
import com.rivan.neon.filters.params.BrightnessFilterParams;
import com.rivan.neon.filters.params.ContrastFilterParams;
import com.rivan.neon.filters.params.GrayscaleFilterParams;

/**
 * A utility class responsible for applying various image filters to a {@link Bitmap}
 * and measuring the time taken for filter application.
 */
public class FilterProcessor {

    /**
     * Applies a specified {@link Filter} to a given {@link Bitmap}, using either
     * Java or Assembly implementation based on the {@code useAssembly} flag.
     * The original bitmap is not modified; a copy is processed and returned.
     *
     * @param bitmap      The original {@link Bitmap} to which the filter will be applied.
     *                    Must not be null and not recycled.
     * @param filter      The {@link Filter} to apply.
     * @param params      Optional {@link FilterParams} specific to the filter. If null,
     *                    default parameters for the filter will be used.
     * @param useAssembly {@code true} to use the Assembly implementation,
     *                    {@code false} to use the Java implementation.
     *
     * @return A new {@link Bitmap} with the applied filter, or {@code null} if the input bitmap
     * is invalid.
     */
    public static Bitmap applyFilterToBitmap(@NonNull Bitmap bitmap, @Nullable Filter filter,
                                             @Nullable FilterParams params, boolean useAssembly) {
        if (bitmap.isRecycled()) {
            return null;
        }

        // Create a copy of the original bitmap to apply the filter to.
        Bitmap processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // If no filter is specified, return the copied original bitmap
        if (filter == null) {
            return processedBitmap;
        }

        switch (filter) {
            case GRAYSCALE:
                GrayscaleFilterParams grayscaleParams = (GrayscaleFilterParams) params;
                if (grayscaleParams == null) {
                    grayscaleParams = new GrayscaleFilterParams();
                }
                if (useAssembly) {
                    NativeFilters.applyGrayscale(processedBitmap,
                            grayscaleParams.getRedCoefficient(),
                            grayscaleParams.getGreenCoefficient(),
                            grayscaleParams.getBlueCoefficient());
                } else {
                    processedBitmap = JavaFilters.applyGrayscale(processedBitmap,
                            grayscaleParams.getRedCoefficient(),
                            grayscaleParams.getGreenCoefficient(),
                            grayscaleParams.getBlueCoefficient());
                }
                break;
            case INVERT:
                if (useAssembly) {
                    NativeFilters.applyInvert(processedBitmap);
                } else {
                    processedBitmap = JavaFilters.applyInvert(processedBitmap);
                }
                break;
            case BRIGHTNESS:
                BrightnessFilterParams brightnessParams = (BrightnessFilterParams) params;
                if (brightnessParams == null) {
                    brightnessParams = new BrightnessFilterParams();
                }
                // Apply the filter only if the value is not default (0)
                if (brightnessParams.getBrightness() != 0) {
                    if (useAssembly) {
                        NativeFilters.applyBrightness(processedBitmap, brightnessParams.getBrightness());
                    } else {
                        processedBitmap = JavaFilters.applyBrightness(processedBitmap,
                                brightnessParams.getBrightness());
                    }
                }
                break;
            case CONTRAST:
                ContrastFilterParams contrastParams = (ContrastFilterParams) params;
                if (contrastParams == null) {
                    contrastParams = new ContrastFilterParams();
                }
                // Apply the filter only if the value is not default (1.0)
                if (contrastParams.getContrast() != 1.0f) {
                    if (useAssembly) {
                        NativeFilters.applyContrast(processedBitmap, contrastParams.getContrast());
                    } else {
                        processedBitmap = JavaFilters.applyContrast(processedBitmap,
                                contrastParams.getContrast());
                    }
                }
                break;
            case SEPIA:
                if (useAssembly) {
                    NativeFilters.applySepia(processedBitmap);
                } else {
                    processedBitmap = JavaFilters.applySepia(processedBitmap);
                }
                break;
            default:
                break;
        }

        return processedBitmap;
    }

    /**
     * Measures the time taken to apply a specified {@link Filter} to a given {@link Bitmap}.
     * This method supports both Java and Assembly implementations.
     * For Java, it measures the execution time of {@link #applyFilterToBitmap};
     * for Assembly, it calls a dedicated measurement function in {@code NativeFilters}.
     *
     * @param bitmap      The original {@link Bitmap} for which to measure filter application time.
     *                    Must not be null and not recycled.
     * @param filter      The {@link Filter} to measure.
     * @param params      Optional {@link FilterParams} for the filter. If null, default
     *                    parameters for the filter will be used.
     * @param useAssembly {@code true} to measure the Native (Assembly) implementation,
     *                    {@code false} to measure the Java implementation.
     *
     * @return The duration of the filter application in nanoseconds (long), or -1 if
     * the input bitmap or filter is invalid, or if the filter is not applied
     * (e.g., brightness 0, contrast 1.0f).
     */
    public static long measureFilterTime(@NonNull Bitmap bitmap, @Nullable Filter filter,
                                         @Nullable FilterParams params, boolean useAssembly) {
        if (bitmap.isRecycled()) {
            return -1;
        }

        if (filter == null) {
            return -1;
        }

        long measuredDurationNs;

        if (useAssembly) {
            switch (filter) {
                case GRAYSCALE:
                    GrayscaleFilterParams grayscaleParams = (GrayscaleFilterParams) params;
                    if (grayscaleParams == null) {
                        grayscaleParams = new GrayscaleFilterParams();
                    }
                    measuredDurationNs = NativeFilters.measureGrayscale(bitmap,
                            grayscaleParams.getRedCoefficient(),
                            grayscaleParams.getGreenCoefficient(),
                            grayscaleParams.getGreenCoefficient());
                    break;
                case INVERT:
                    measuredDurationNs = NativeFilters.measureInvert(bitmap);
                    break;
                case BRIGHTNESS:
                    BrightnessFilterParams brightnessParams = (BrightnessFilterParams) params;
                    if (brightnessParams == null) {
                        brightnessParams = new BrightnessFilterParams();
                    }
                    // Only measure if brightness actually changes the image
                    if (brightnessParams.getBrightness() != 0) {
                        measuredDurationNs = NativeFilters.measureBrightness(bitmap,
                                brightnessParams.getBrightness());
                    } else {
                        measuredDurationNs = -1;
                    }
                    break;
                case CONTRAST:
                    ContrastFilterParams contrastParams = (ContrastFilterParams) params;
                    if (contrastParams == null) {
                        contrastParams = new ContrastFilterParams();
                    }
                    // Only measure if contrast actually changes the image
                    if (contrastParams.getContrast() != 1.0f) {
                        measuredDurationNs = NativeFilters.measureContrast(bitmap,
                                contrastParams.getContrast());
                    } else {
                        measuredDurationNs = -1;
                    }
                    break;
                case SEPIA:
                    measuredDurationNs = NativeFilters.measureSepia(bitmap);
                    break;
                default:
                    measuredDurationNs = -1;
                    break;
            }
        } else {
            // Java implementations are measured by timing the applyFilterToBitmap call
            long startTimeNs = System.nanoTime();
            Bitmap result = applyFilterToBitmap(bitmap, filter, params, false);
            long endTimeNs = System.nanoTime();
            measuredDurationNs = endTimeNs - startTimeNs;

            if (result != null && !result.isRecycled() && result != bitmap) {
                result.recycle();
            }
        }

        return measuredDurationNs;
    }
}
