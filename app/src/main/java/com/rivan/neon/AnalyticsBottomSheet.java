package com.rivan.neon;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.rivan.neon.databinding.AnalyticsBottomSheetLayoutBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AnalyticsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AnalyticsBottomSheet";

    private AnalyticsBottomSheetLayoutBinding binding;

    private MainActivityViewModel viewModel;

    private LinearLayout progressBarsContainer;
    private TextView noDataMessageTextView;

    private final DecimalFormat decimalFormat = new DecimalFormat("###,##0.00 ms");

    private Map<Integer, Filter> filterChipIdToEnumMap;
    private Map<Integer, String> languageChipIdToNameMap;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);

        filterChipIdToEnumMap = new HashMap<>();
        filterChipIdToEnumMap.put(R.id.grayscale_filter_chip, Filter.GRAYSCALE);
        filterChipIdToEnumMap.put(R.id.invert_filter_chip, Filter.INVERT);
        filterChipIdToEnumMap.put(R.id.brightness_filter_chip, Filter.BRIGHTNESS);
        filterChipIdToEnumMap.put(R.id.contrast_filter_chip, Filter.CONTRAST);
        filterChipIdToEnumMap.put(R.id.sepia_filter_chip, Filter.SEPIA);

        languageChipIdToNameMap = new HashMap<>();
        languageChipIdToNameMap.put(R.id.java_language_chip, "Java");
        languageChipIdToNameMap.put(R.id.asm_language_chip, "Assembly");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = AnalyticsBottomSheetLayoutBinding.inflate(inflater, container, false);

        progressBarsContainer = binding.progressBarsContainer;
        noDataMessageTextView = binding.noDataMessageTextView;

        binding.filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> updateProgressBars());
        binding.languageChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> updateProgressBars());

        viewModel.getBenchmarkResults().observe(this, benchmarkResults -> updateProgressBars());

        if (getDialog() != null) {
            getDialog().setOnShowListener(dialog -> {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheetInternal != null) {
                    BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            });
        }

        binding.getRoot().post(() -> { // Post to ensure layout is measured
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            int desiredHeight = (int) (displayHeight * 0.80); // 80% of screen height
            ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
            if (params != null) {
                params.height = desiredHeight;
                binding.getRoot().setLayoutParams(params);
            }
        });

        updateProgressBars();

        return binding.getRoot();
    }

    private void updateProgressBars() {
        List<BenchmarkResult> allBenchmarkResults = viewModel.getBenchmarkResults().getValue();

        progressBarsContainer.removeAllViews();
        noDataMessageTextView.setVisibility(View.GONE);

        if (allBenchmarkResults == null || allBenchmarkResults.isEmpty()) {
            showNoDataMessage(R.string.analytics_no_benchmark);
            return;
        }

        Set<Filter> selectedFilterEnums = new HashSet<>();
        for (int id : binding.filterChipGroup.getCheckedChipIds()) {
            Filter filter = filterChipIdToEnumMap.get(id);
            if (filter != null) {
                selectedFilterEnums.add(filter);
            }
        }

        if (selectedFilterEnums.isEmpty() && !binding.filterChipGroup.getCheckedChipIds().isEmpty()) {
            selectedFilterEnums.addAll(filterChipIdToEnumMap.values());
        } else if (binding.filterChipGroup.getCheckedChipIds().isEmpty()) {
            selectedFilterEnums.addAll(filterChipIdToEnumMap.values());
        }

        Set<String> selectedLanguages = new HashSet<>();
        for (int id : binding.languageChipGroup.getCheckedChipIds()) {
            String language = languageChipIdToNameMap.get(id);
            if (language != null) {
                selectedLanguages.add(language);
            }
        }

        if (selectedLanguages.isEmpty() && !binding.languageChipGroup.getCheckedChipIds().isEmpty()) {
            selectedLanguages.add("Java");
            selectedLanguages.add("Assembly");
        } else if (binding.languageChipGroup.getCheckedChipIds().isEmpty()) {
            selectedLanguages.add("Java");
            selectedLanguages.add("Assembly");
        }

        List<BenchmarkResult> filteredResults = allBenchmarkResults.stream()
                .filter(result -> selectedFilterEnums.contains(mapStringToFilter(result.getFilterName())) &&
                        selectedLanguages.contains(result.getLanguage()))
                .collect(Collectors.toList());

        if (filteredResults.isEmpty()) {
            showNoDataMessage(R.string.analytics_no_data);
            return;
        }

        Map<String, List<BenchmarkResult>> groupedByFilter = filteredResults.stream()
                .collect(Collectors.groupingBy(BenchmarkResult::getFilterName));

        // Sort filter names for consistent display order
        List<String> sortedFilterNames = new ArrayList<>(groupedByFilter.keySet());
        Collections.sort(sortedFilterNames);

        // Determine a maximum value for the progress bar to normalize them
        // This could be the highest average time among filtered results, or a fixed max.
        double maxTimeMs = filteredResults.stream()
                .mapToDouble(BenchmarkResult::getAverageTimeMs)
                .max()
                .orElse(1.0); // Avoid division by zero if all times are 0 or list is empty (shouldn't happen here)

        // Ensure maxTimeMs is never zero or negative to avoid issues with ProgressBar scale
        if (maxTimeMs <= 0) {
            maxTimeMs = 1.0;
        }

        for (String filterName : sortedFilterNames) {
            List<BenchmarkResult> filterResults = groupedByFilter.get(filterName);
            // Sort results within the filter group by language (Java then Assembly) for consistency
            filterResults.sort(Comparator.comparing(BenchmarkResult::getLanguage));

            addBenchmarkItem(filterName, filterResults, maxTimeMs);
        }
    }

    private void addBenchmarkItem(String filterName, List<BenchmarkResult> resultsForFilter, double maxTimeMs) {
        // Inflate the new custom layout for each filter item
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.filter_benchmark_item, progressBarsContainer, false);

        TextView filterNameTextView = itemView.findViewById(R.id.filterNameTextView);
        LinearLayout javaBenchmarkRow = itemView.findViewById(R.id.javaBenchmarkRow);
        LinearProgressIndicator javaProgressBar = itemView.findViewById(R.id.javaProgressBar);
        TextView javaTimeTextView = itemView.findViewById(R.id.javaTimeTextView);

        LinearLayout assemblyBenchmarkRow = itemView.findViewById(R.id.assemblyBenchmarkRow);
        LinearProgressIndicator assemblyProgressBar = itemView.findViewById(R.id.assemblyProgressBar);
        TextView assemblyTimeTextView = itemView.findViewById(R.id.assemblyTimeTextView);

        filterNameTextView.setText(filterName);

        // Find Java and Assembly results
        BenchmarkResult javaResult = null;
        BenchmarkResult assemblyResult = null;
        for (BenchmarkResult result : resultsForFilter) {
            if ("Java".equals(result.getLanguage())) {
                javaResult = result;
            } else if ("Assembly".equals(result.getLanguage())) {
                assemblyResult = result;
            }
        }

        // Populate Java row
        if (javaResult != null) {
            javaBenchmarkRow.setVisibility(View.VISIBLE);
            javaProgressBar.setMax((int) (maxTimeMs * 100));
            javaProgressBar.setProgress((int) (javaResult.getAverageTimeMs() * 100));
            javaTimeTextView.setText(decimalFormat.format(javaResult.getAverageTimeMs()));
        } else {
            javaBenchmarkRow.setVisibility(View.GONE); // Hide if no Java data for this filter
        }

        // Populate Assembly row
        if (assemblyResult != null) {
            assemblyBenchmarkRow.setVisibility(View.VISIBLE);
            assemblyProgressBar.setMax((int) (maxTimeMs * 100));
            assemblyProgressBar.setProgress((int) (assemblyResult.getAverageTimeMs() * 100));
            assemblyTimeTextView.setText(decimalFormat.format(assemblyResult.getAverageTimeMs()));
        } else {
            assemblyBenchmarkRow.setVisibility(View.GONE); // Hide if no Assembly data for this filter
        }

        progressBarsContainer.addView(itemView);
    }

    private void showNoDataMessage(@StringRes int messageRes) {
        noDataMessageTextView.setText(messageRes);
        noDataMessageTextView.setVisibility(View.VISIBLE);
    }

    private Filter mapStringToFilter(String filterName) {
        try {
            return Filter.valueOf(filterName.toUpperCase());
        } catch (IllegalArgumentException e) {
             Log.e(TAG, "Unknown filter name: " + filterName, e); // Uncomment for debugging
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
