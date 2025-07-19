package com.rivan.neon;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.loadingindicator.LoadingIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the ViewPager containing the filter previews.
 */
public class FilterPreviewAdapter extends RecyclerView.Adapter<FilterPreviewAdapter.ViewHolder> {

    private List<FilterPreviewItem> filterPreviewItems;

    public FilterPreviewAdapter() {
        this.filterPreviewItems = new ArrayList<>();
    }

    public void updateAllPreviews(List<FilterPreviewItem> newPreviewItems) {
        // Calculate the difference between the old and new lists
        FilterPreviewDiffCallback diffCallback =
                new FilterPreviewDiffCallback(this.filterPreviewItems, newPreviewItems);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        // Update the adapter's internal list
        this.filterPreviewItems = newPreviewItems;

        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.item_filter_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < filterPreviewItems.size()) {
            FilterPreviewItem item = filterPreviewItems.get(position);

            // Show a loading indicator when the preview is still loading
            if (item.isLoading()) {
                holder.filterLoadingIndicator.setVisibility(View.VISIBLE);
                holder.filterNameOverlay.setVisibility(View.INVISIBLE);
                holder.filterPreviewImage.setVisibility(View.INVISIBLE);
            } else {
                holder.filterLoadingIndicator.setVisibility(View.GONE);
                holder.filterNameOverlay.setVisibility(View.VISIBLE);
                holder.filterPreviewImage.setVisibility(View.VISIBLE);
            }

            holder.filterNameOverlay.setText(item.getFilter().getTitleRes());
            holder.filterPreviewImage.setImageBitmap(item.getPreviewBitmap());

        } else {
            holder.filterLoadingIndicator.setVisibility(View.VISIBLE);
            holder.filterNameOverlay.setVisibility(View.INVISIBLE);
            holder.filterPreviewImage.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return filterPreviewItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView filterPreviewImage;
        TextView filterNameOverlay;
        LoadingIndicator filterLoadingIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            filterPreviewImage = itemView.findViewById(R.id.filter_preview_image);
            filterNameOverlay = itemView.findViewById(R.id.filter_name_overlay);
            filterLoadingIndicator = itemView.findViewById(R.id.preview_item_loading_indicator);
        }
    }
}
