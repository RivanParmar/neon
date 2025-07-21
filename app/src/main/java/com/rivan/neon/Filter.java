package com.rivan.neon;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.rivan.neon.filters.params.BrightnessFilterParams;
import com.rivan.neon.filters.params.ContrastFilterParams;
import com.rivan.neon.filters.params.GrayscaleFilterParams;

/**
 * Defines the various image filters available in the application. Each filter
 * is associated with a display title resource and, optionally, a class
 * that holds its configurable parameters (implementing {@link FilterParams}).
 *
 * @see FilterParams
 */
public enum Filter {
    ORIGINAL(R.string.filter_original, null),
    GRAYSCALE(R.string.filter_grayscale, GrayscaleFilterParams.class),
    INVERT(R.string.filter_invert, null),
    BRIGHTNESS(R.string.filter_brightness, BrightnessFilterParams.class),
    CONTRAST(R.string.filter_contrast, ContrastFilterParams.class),
    SEPIA(R.string.filter_sepia, null);

    private final int titleRes;

    private final Class<? extends FilterParams> paramsClass;

    Filter(@StringRes int titleRes, @Nullable Class<? extends FilterParams> paramsClass) {
        this.titleRes = titleRes;
        this.paramsClass = paramsClass;
    }

    public int getTitleRes() {
        return titleRes;
    }

    /**
     * Creates and returns a new instance of the default parameters for this filter,
     * if a {@link FilterParams} class is associated with it.
     *
     * @return A new {@link FilterParams} instance with default values, or {@code null}
     * if this filter has no associated parameters.
     */
    public FilterParams createDefaultParams() {
        if (paramsClass == null) {
            return null;
        }
        try {
            return paramsClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Log.e("Filter", e.toString());
            return null;
        }
    }
}
