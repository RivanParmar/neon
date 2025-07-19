package com.rivan.neon.filters.params;

import com.rivan.neon.Filter;
import com.rivan.neon.FilterParams;

/**
 * Parameters that can be adjusted for the Contrast filter.
 *
 * @see com.rivan.neon.FilterParams
 */
public class ContrastFilterParams implements FilterParams {

    public static final float DEFAULT_CONTRAST = 1.0f;

    public static final float MIN_CONTRAST = 0.5f;
    public static final float MAX_CONTRAST = 2.0f;

    private float contrast;

    public ContrastFilterParams() {
        contrast = DEFAULT_CONTRAST;
    }

    public float getContrast() {
        return contrast;
    }

    public void setContrast(float contrast) {
        this.contrast = contrast;
    }

    @Override
    public Filter getFilterType() {
        return Filter.CONTRAST;
    }

    @Override
    public FilterParams copy() {
        ContrastFilterParams copy = new ContrastFilterParams();
        copy.setContrast(contrast);
        return copy;
    }
}
