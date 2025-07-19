package com.rivan.neon;

import android.graphics.Bitmap;

import java.util.Objects;

/**
 * Data class representing a single filter's preview item within a ViewPager.
 * It holds information about the filter itself, its generated preview bitmap,
 * and its current loading state.
 */
public class FilterPreviewItem {

    private final Filter filter;
    private Bitmap previewBitmap;
    private boolean isLoading;

    public FilterPreviewItem(Filter filter, Bitmap previewBitmap, boolean isLoading) {
        this.filter = filter;
        this.previewBitmap = previewBitmap;
        this.isLoading = isLoading;
    }

    public Filter getFilter() {
        return filter;
    }

    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public void setPreviewBitmap(Bitmap previewBitmap) {
        this.previewBitmap = previewBitmap;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterPreviewItem that = (FilterPreviewItem) o;
        // Two FilterPreviewItems are considered equal if they represent the same Filter type.
        // This is crucial for RecyclerView.Adapter.DiffUtil for identifying items.
        return filter == that.filter;
    }

    @Override
    public int hashCode() {
        // Generates a hash code based solely on the 'filter' field,
        // which serves as the unique identifier for the item.
        return Objects.hash(filter);
    }

    /**
     * Compares the content of this {@link FilterPreviewItem} with another,
     * specifically checking if their {@code previewBitmap} and {@code isLoading} states are identical.
     * This method can be used to determine if an item's visual representation has changed,
     * triggering a UI update.
     *
     * @param other The other {@link FilterPreviewItem} to compare content with.
     *
     * @return {@code true} if the preview bitmaps are pixel-identical (or both null)
     * and the loading states are the same; {@code false} otherwise.
     */
    public boolean contentEquals(FilterPreviewItem other) {
        if (other == null) return false;
        boolean bitmapEquals;
        if (this.previewBitmap != null && other.previewBitmap != null) {
            bitmapEquals = this.previewBitmap.sameAs(other.previewBitmap); // Pixel-level comparison
        } else if (this.previewBitmap == null && other.previewBitmap == null) {
            bitmapEquals = true; // Both are null, so considered equal
        } else {
            bitmapEquals = false; // One is null, the other is not
        }

        // Also compare loading state
        return bitmapEquals && (this.isLoading == other.isLoading());
    }
}
