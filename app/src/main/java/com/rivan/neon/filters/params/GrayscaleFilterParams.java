package com.rivan.neon.filters.params;

import com.rivan.neon.Filter;
import com.rivan.neon.FilterParams;

/**
 * Parameters that can be adjusted for the Grayscale filter.
 *
 * @see com.rivan.neon.FilterParams
 */
public class GrayscaleFilterParams implements FilterParams {

    public static final float DEFAULT_RED_COEFFICIENT = 0.299f;
    public static final float DEFAULT_GREEN_COEFFICIENT = 0.587f;
    public static final float DEFAULT_BLUE_COEFFICIENT = 0.114f;

    public static final float MIN_COEFFICIENT = 0.00f;
    public static final float MAX_COEFFICIENT = 1.00f;

    private float redCoefficient;
    private float greenCoefficient;
    private float blueCoefficient;

    public GrayscaleFilterParams() {
        redCoefficient = DEFAULT_RED_COEFFICIENT;
        greenCoefficient = DEFAULT_GREEN_COEFFICIENT;
        blueCoefficient = DEFAULT_BLUE_COEFFICIENT;
    }

    public float getRedCoefficient() {
        return redCoefficient;
    }

    public float getGreenCoefficient() {
        return greenCoefficient;
    }

    public float getBlueCoefficient() {
        return blueCoefficient;
    }

    public void setRedCoefficient(float redCoefficient) {
        this.redCoefficient = redCoefficient;
    }

    public void setGreenCoefficient(float greenCoefficient) {
        this.greenCoefficient = greenCoefficient;
    }

    public void setBlueCoefficient(float blueCoefficient) {
        this.blueCoefficient = blueCoefficient;
    }

    @Override
    public Filter getFilterType() {
        return Filter.GRAYSCALE;
    }

    @Override
    public FilterParams copy() {
        GrayscaleFilterParams copy = new GrayscaleFilterParams();
        copy.setRedCoefficient(redCoefficient);
        copy.setGreenCoefficient(greenCoefficient);
        copy.setBlueCoefficient(blueCoefficient);
        return copy;
    }
}
