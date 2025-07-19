package com.rivan.neon;

/**
 * <p>
 * Marker interface for defining a set of parameters specific to a particular image filter.
 * Implementing classes will hold the configurable values (e.g., brightness level, contrast factor)
 * that define how a filter modifies an image.
 * </p>
 *
 * <p>
 * Note that implementing classes need to be passed to the respective {@link Filter}'s
 * {@code paramsClass} variable for the parameters to be visible.
 * Additionally, the <b>constructor of implementing classes should reset the parameters' values</b>
 * to their default state when called.
 * </p>
 */
public interface FilterParams {

    Filter getFilterType();

    FilterParams copy();
}
