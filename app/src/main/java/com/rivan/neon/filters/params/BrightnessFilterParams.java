package com.rivan.neon.filters.params;

import com.rivan.neon.Filter;
import com.rivan.neon.FilterParams;

/**
 * Parameters that can be adjusted for the Brightness filter.
 *
 * @see com.rivan.neon.FilterParams
 */
public class BrightnessFilterParams implements FilterParams {

    public static final int DEFAULT_BRIGHTNESS = 0;

    public static final int MIN_BRIGHTNESS = -100;
    public static final int MAX_BRIGHTNESS = 100;

    private int brightness;

    public BrightnessFilterParams() {
        brightness = DEFAULT_BRIGHTNESS;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    @Override
    public Filter getFilterType() {
        return Filter.BRIGHTNESS;
    }

    @Override
    public FilterParams copy() {
        BrightnessFilterParams copy = new BrightnessFilterParams();
        copy.setBrightness(brightness);
        return copy;
    }
}
