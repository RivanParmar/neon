package com.rivan.neon;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class FilterPreviewDiffCallback extends DiffUtil.Callback {

    private final List<FilterPreviewItem> oldList;
    private final List<FilterPreviewItem> newList;

    public FilterPreviewDiffCallback(List<FilterPreviewItem> oldList, List<FilterPreviewItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    // Called to check whether two objects represent the same item.
    // In our case, the 'filter' enum value uniquely identifies each preview item.
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getFilter() == newList.get(newItemPosition).getFilter();
    }

    // Called to check whether two items have the same data (content).
    // This is called only if areItemsTheSame() returns true.
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).contentEquals(newList.get(newItemPosition));
    }
}
