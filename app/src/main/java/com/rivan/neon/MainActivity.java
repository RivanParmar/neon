package com.rivan.neon;

import androidx.activity.BackEventCompat;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.rivan.neon.databinding.ActivityMainBinding;
import com.rivan.neon.filters.params.BrightnessFilterParams;
import com.rivan.neon.filters.params.ContrastFilterParams;
import com.rivan.neon.filters.params.GrayscaleFilterParams;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private boolean[] selectedFilters = new boolean[5];
    private boolean[] selectedLanguages = new boolean[2];

    // Used to load the 'neon' library on application startup.
    static {
        System.loadLibrary("neon");
    }

    private ActivityMainBinding binding;

    private MainActivityViewModel viewModel;

    /** {@link ActivityResultLauncher} used to show the photo picker. */
    private ActivityResultLauncher<PickVisualMediaRequest> pickImage;

    private LinearLayout paramsContainer;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private AlertDialog filtersDialog, languagesDialog;

    private AnalyticsBottomSheet analyticsBottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        analyticsBottomSheet = new AnalyticsBottomSheet();

        pickImage = registerForActivityResult(new PickVisualMedia(), uri -> {
            if (uri != null) {
                int targetWidth = getResources().getDisplayMetrics().widthPixels;
                int targetHeight = getResources().getDisplayMetrics().heightPixels;

                viewModel.loadImageFromUri(uri, targetWidth, targetHeight);
                viewModel.clearAllBenchmarkResults();
            }
        });

        MaterialToolbar topAppBar = binding.topAppBar;
        topAppBar.setOnMenuItemClickListener((menuItem) -> {
            if (menuItem.getItemId() == R.id.open_image) {
                pickImage.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
                return true;
            } else if (menuItem.getItemId() == R.id.analytics) {
                analyticsBottomSheet.show(getSupportFragmentManager(), AnalyticsBottomSheet.TAG);
            }
            return false;
        });

        FilterPreviewAdapter adapter = new FilterPreviewAdapter();

        ViewPager2 filterViewPager = binding.filterViewPager;
        filterViewPager.setOffscreenPageLimit(1);
        filterViewPager.setAdapter(adapter);

        float nextItemVisiblePx = getResources().getDimension(R.dimen.viewpager_next_item_visible);
        float currentItemHorizontalMarginPx = getResources().getDimension(R.dimen.viewpager_current_item_horizontal_margin);

        float pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx;
        ViewPager2.PageTransformer pageTransformer = (page, position) -> {
            page.setTranslationX(-pageTranslationX * position);
            page.setScaleY(1 - (0.25f * Math.abs(position)));
        };
        filterViewPager.setPageTransformer(pageTransformer);
        filterViewPager.addItemDecoration(new HorizontalMarginItemDecoration(
                this, R.dimen.viewpager_current_item_horizontal_margin));

        MaterialButton resetFilterButton = binding.paramsBottomSheet.resetFilterButton;
        MaterialButton saveImageButton = binding.buttonSaveImage;
        MaterialButton adjustFilterButton = binding.buttonAdjustFilterParams;
        MaterialButton individualBenchmarkButton = binding.buttonBenchmarkIndividual;
        MaterialButton compareBenchmarkButton = binding.buttonBenchmarkCompare;

        TextView selectImageTextView = binding.selectImagePrompt;
        selectImageTextView.setVisibility(View.VISIBLE);

        viewModel.getOriginalBitmap().observe(this, bitmap -> {
            if (bitmap != null) {
                filterViewPager.setCurrentItem(0, false);
                selectImageTextView.setVisibility(View.GONE);
            } else {
                selectImageTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getAllPreviews().observe(this, newPreviewItems -> {
            if (newPreviewItems != null && !newPreviewItems.isEmpty()) {
                adapter.updateAllPreviews(newPreviewItems);
            } else {
                adapter.updateAllPreviews(new ArrayList<>()); // Clear adapter if no previews
            }
            // After any update (initial load, single filter, reapply),
            // ensure ViewPager2 is correctly positioned and enabled.
            // Using post to ensure it runs after RecyclerView has processed its layout.
            filterViewPager.post(() -> {
                int currentItem = filterViewPager.getCurrentItem();
                filterViewPager.setCurrentItem(currentItem, false); // Re-center current item
                filterViewPager.setUserInputEnabled(true); // Ensure enabled after any update
            });
        });

        viewModel.shouldUseAssembly().observe(this, shouldUseAssembly -> {
            viewModel.reapplyAllFilters(); // This will update _allPreviews in ViewModel

            int currentPos = filterViewPager.getCurrentItem();
            if (currentPos >= 0 && currentPos < viewModel.getAllFilters().size()) {
                Filter currentFilter = viewModel.getAllFilters().get(currentPos);
                displayFilterParams(currentFilter);
            }
        });

        filterViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                viewModel.setCurrentFilter(position);
            }
        });

        viewModel.getCurrentFilterPosition().observe(this, position -> {
            if (position != null && position >= 0 && position < viewModel.getAllFilters().size()) {
                Filter currentFilter = viewModel.getAllFilters().get(position);

                // Can't save the original image again
                saveImageButton.setEnabled(currentFilter != Filter.ORIGINAL);

                // Can't benchmark the original image
                individualBenchmarkButton.setEnabled(currentFilter != Filter.ORIGINAL);

                // Hide the "Reset" button when no adjustable parameters are there
                if (currentFilter == Filter.ORIGINAL || currentFilter == Filter.INVERT ||
                        currentFilter == Filter.SEPIA) {
                    resetFilterButton.setVisibility(View.GONE);
                } else {
                    resetFilterButton.setVisibility(View.VISIBLE);
                }
                displayFilterParams(currentFilter);
            }
        });

        paramsContainer = binding.paramsBottomSheet.dynamicSlidersContainer;

        LinearLayout paramsBottomSheet = binding.paramsBottomSheet.filterParamsBottomSheet;
        BottomSheetBehavior<LinearLayout> paramsBottomSheetBehaviour = BottomSheetBehavior.from(paramsBottomSheet);
        paramsBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_HIDDEN);
        int maxHeight = getResources().getDimensionPixelOffset(R.dimen.filter_bottom_sheet_peek_height);
        paramsBottomSheetBehaviour.setPeekHeight(maxHeight);
        paramsBottomSheetBehaviour.setMaxHeight(maxHeight);

        // Callback for the bottom sheet to make it work with predictive back gesture.
        // No need for this for a modal bottom sheet.
        OnBackPressedCallback bottomSheetBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackStarted(@NonNull BackEventCompat backEvent) {
                paramsBottomSheetBehaviour.startBackProgress(backEvent);
            }

            @Override
            public void handleOnBackProgressed(@NonNull BackEventCompat backEvent) {
                paramsBottomSheetBehaviour.updateBackProgress(backEvent);
            }

            @Override
            public void handleOnBackPressed() {
                paramsBottomSheetBehaviour.handleBackInvoked();
            }

            @Override
            public void handleOnBackCancelled() {
                paramsBottomSheetBehaviour.cancelBackProgress();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, bottomSheetBackCallback);

        paramsBottomSheetBehaviour.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED ||
                        newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    bottomSheetBackCallback.setEnabled(true);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED ||
                        newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBackCallback.setEnabled(false);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        adjustFilterButton.setOnClickListener(view -> {
            paramsBottomSheetBehaviour.setState(BottomSheetBehavior.STATE_EXPANDED);
            int currentPos = filterViewPager.getCurrentItem();
            if (currentPos >= 0 && currentPos < viewModel.getAllFilters().size()) {
                Filter currentFilter = viewModel.getAllFilters().get(currentPos);
                displayFilterParams(currentFilter);
            }
        });

        resetFilterButton.setOnClickListener(v -> {
            Filter currentFilter = getCurrentFilter();
            if (currentFilter == null) {
                return;
            }

            if (currentFilter == Filter.ORIGINAL || currentFilter == Filter.INVERT ||
                    currentFilter == Filter.SEPIA) {
                return;
            }

            // Get the default parameters for the current filter
            FilterParams defaultParams = currentFilter.createDefaultParams();
            if (defaultParams == null) {
                // This should ideally not happen if createDefaultParams() is implemented for all
                // adjustable filters
                return;
            }

            // Update the ViewModel with default parameters
            viewModel.updateFilterParams(currentFilter, defaultParams);

            // Refresh the bottom sheet UI to reflect the default values
            displayFilterParams(currentFilter);
        });

        MaterialButtonToggleGroup toggleGroup = binding.languageSelectorToggleGroup;

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.button_lang_assembly && isChecked) {
                viewModel.setUseAssembly(true);
            } else if (checkedId == R.id.button_lang_java && isChecked) {
                viewModel.setUseAssembly(false);
            }
            filterViewPager.setCurrentItem(0, true);
        });

        saveImageButton.setOnClickListener(view -> {
            Filter currentFilter = getCurrentFilter();
            if (currentFilter == null) {
                return;
            }
            FilterParams currentFilterParams = viewModel.getFilterParams().getValue() != null ?
                    viewModel.getFilterParams().getValue().get(currentFilter) : null;

            Bitmap original = viewModel.getOriginalBitmap().getValue();
            if (original == null) {
                return;
            }

            Bitmap filteredBitmap = FilterProcessor.applyFilterToBitmap(original, currentFilter, currentFilterParams,
                    viewModel.shouldUseAssembly().getValue() != null && viewModel.shouldUseAssembly().getValue());

            executorService.execute(() -> saveBitmapToPictures(filteredBitmap));
        });

        individualBenchmarkButton.setOnClickListener(view -> {
            Map<Filter, FilterParams> filterParamsMap = viewModel.getFilterParams().getValue();
            Filter currentFilter = getCurrentFilter();
            if (currentFilter == null) {
                return;
            }
            FilterParams currentFilterParams = getFilterParamsForFilter(currentFilter, filterParamsMap);

            if (currentFilterParams instanceof BrightnessFilterParams &&
                    ((BrightnessFilterParams) currentFilterParams).getBrightness() == 0) {
                Snackbar.make(view, R.string.adjust_brightness_value_text, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.adjust, snackbarView -> adjustFilterButton.callOnClick())
                        .show();
            } else if (currentFilterParams instanceof ContrastFilterParams &&
            ((ContrastFilterParams) currentFilterParams).getContrast() == 1.0f) {
                Snackbar.make(view, R.string.adjust_contrast_value_text, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.adjust, snackbarView -> adjustFilterButton.callOnClick())
                        .show();
            } else {
                runBenchmarks(false);
            }
        });

        compareBenchmarkButton.setOnClickListener(view -> {
            showFilterSelectionDialog(this);
        });
    }

    /**
     * Used to display the {@link FilterParams} for a given {@link Filter}.
     */
    private void displayFilterParams(@NonNull Filter filter) {
        paramsContainer.removeAllViews();

        if (filter == Filter.ORIGINAL) {
            TextView noParamsText = new TextView(this);
            noParamsText.setText(R.string.filter_original_no_params_text);
            noParamsText.setPadding(16, 8, 16, 16);
            noParamsText.setGravity(Gravity.CENTER);
            paramsContainer.addView(noParamsText);
            return;
        }

        Map<Filter, FilterParams> currentParamsMap = viewModel.getFilterParams().getValue();
        FilterParams currentParams = (currentParamsMap != null) ? currentParamsMap.get(filter) : null;

        if (currentParams == null) {
            TextView noParamsText = new TextView(this);
            noParamsText.setText(getString(R.string.filter_no_params_text, getString(filter.getTitleRes())));
            noParamsText.setPadding(16, 8, 16, 16);
            noParamsText.setGravity(Gravity.CENTER);
            paramsContainer.addView(noParamsText);
            return;
        }

        if (filter == Filter.GRAYSCALE) {
            GrayscaleFilterParams grayscaleParams = (GrayscaleFilterParams) currentParams;
            addParams(paramsContainer, R.string.red_coefficient, GrayscaleFilterParams.MIN_COEFFICIENT,
                    GrayscaleFilterParams.MAX_COEFFICIENT, 0.01f, grayscaleParams.getRedCoefficient(),
                    value -> {
                        grayscaleParams.setRedCoefficient(value);
                        return grayscaleParams;
                    }, filter);
            addParams(paramsContainer, R.string.green_coefficient, GrayscaleFilterParams.MIN_COEFFICIENT,
                    GrayscaleFilterParams.MAX_COEFFICIENT, 0.01f, grayscaleParams.getGreenCoefficient(),
                    value -> {
                        grayscaleParams.setGreenCoefficient(value);
                        return grayscaleParams;
                    }, filter);
            addParams(paramsContainer, R.string.blue_coefficient, GrayscaleFilterParams.MIN_COEFFICIENT,
                    GrayscaleFilterParams.MAX_COEFFICIENT, 0.01f, grayscaleParams.getBlueCoefficient(),
                    value -> {
                        grayscaleParams.setBlueCoefficient(value);
                        return grayscaleParams;
                    }, filter);
        } else if (filter == Filter.BRIGHTNESS) {
            BrightnessFilterParams brightnessParams = (BrightnessFilterParams) currentParams;
            addParams(paramsContainer, R.string.filter_brightness, BrightnessFilterParams.MIN_BRIGHTNESS,
                    BrightnessFilterParams.MAX_BRIGHTNESS, 1.0f, brightnessParams.getBrightness(),
                    value -> {
                        brightnessParams.setBrightness((int) value);
                        return brightnessParams;
                    }, filter);
        } else if (filter == Filter.CONTRAST) {
            ContrastFilterParams contrastParams = (ContrastFilterParams) currentParams;
            addParams(paramsContainer, R.string.filter_contrast, ContrastFilterParams.MIN_CONTRAST,
                    ContrastFilterParams.MAX_CONTRAST, 0.1f, contrastParams.getContrast(),
                    value -> {
                        contrastParams.setContrast(value);
                        return contrastParams;
                    }, filter);
        }
    }

    /**
     * Adds UI components that allow modifying the parameter values for a Filter to the given
     * parent layout. Currently, this only adds Sliders to the UI.
     */
    private void addParams(LinearLayout parent, @StringRes int labelRes, float min, float max,
                           float stepSize, float currentValue, ParamUpdater updater, Filter filter) {
        View sliderItemView = LayoutInflater.from(this).inflate(R.layout.slider_filter_param, parent, false);

        TextView labelTextView = sliderItemView.findViewById(R.id.slider_param_text);
        Slider slider = sliderItemView.findViewById(R.id.slider_param);
        MaterialButton minusButton = sliderItemView.findViewById(R.id.minus_button);
        MaterialButton plusButton = sliderItemView.findViewById(R.id.plus_button);

        labelTextView.setText(labelRes);
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setValue(currentValue);

        slider.setStepSize(stepSize);

        float snappedValue = min + Math.round((currentValue - min) / stepSize) * stepSize;
        snappedValue = Math.max(min, Math.min(max, snappedValue));
        slider.setValue(snappedValue);

        slider.setLabelFormatter(value -> {
            // Determine number of decimal places needed based on stepSize
            int decimalPlaces;
            if (stepSize == 0.0f) {
                decimalPlaces = 0;
            } else {
                String stepSizeString = String.valueOf(stepSize);
                int indexOfDecimal = stepSizeString.indexOf('.');
                if (indexOfDecimal == -1) {
                    decimalPlaces = 0;
                } else {
                    decimalPlaces = stepSizeString.length() - 1 - indexOfDecimal;
                }
                decimalPlaces = Math.min(decimalPlaces, 3); // Limit to 3 decimal places
            }

            String formatString = "%." + decimalPlaces + "f";
            return String.format(formatString, value);
        });

        // Add listeners for the new buttons
        minusButton.setOnClickListener(v -> {
            float newValue = slider.getValue() - stepSize;
            if (newValue < min) { // Ensure new value doesn't go below min
                newValue = min;
            }
            slider.setValue(newValue); // Update slider value
            // Manually trigger update if not handled by onStopTrackingTouch from slider setValue
            FilterParams updatedParams = updater.update(slider.getValue());
            viewModel.updateFilterParams(filter, updatedParams);
        });

        plusButton.setOnClickListener(v -> {
            float newValue = slider.getValue() + stepSize;
            if (newValue > max) { // Ensure new value doesn't go above max
                newValue = max;
            }
            slider.setValue(newValue); // Update slider value
            // Manually trigger update
            FilterParams updatedParams = updater.update(slider.getValue());
            viewModel.updateFilterParams(filter, updatedParams);
        });


        // Keep the existing slider touch listener
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                // Only update the value once the user has stopped moving the slider to
                // reduce unnecessary updates
                FilterParams updatedParams = updater.update(slider.getValue());
                viewModel.updateFilterParams(filter, updatedParams);
            }
        });

        parent.addView(sliderItemView);
    }

    private interface ParamUpdater {
        FilterParams update(float newSliderValue);
    }

    /**
     * Saves the given Bitmap to the "Pictures/Neon" folder.
     */
    private void saveBitmapToPictures(Bitmap bitmap) {
        ContentResolver contentResolver = getContentResolver();
        ContentValues contentValues = new ContentValues();

        String displayName = "NeonImage_" + System.currentTimeMillis() + ".jpg";
        String mimeType = "image/jpeg";

        String folderName = "Neon";
        String relativePath = Environment.DIRECTORY_PICTURES + "/" + folderName;

        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);

        Uri newUri = null;
        OutputStream outputStream = null;

        try {
            Uri imageCollection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            } else {
                imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }

            newUri = contentResolver.insert(imageCollection, contentValues);
            if (newUri == null) {
                throw new IOException("Failed to open output stream for new URI.");
            }

            outputStream = contentResolver.openOutputStream(newUri);
            if (outputStream == null) {
                throw new IOException("Failed to open output stream for new URI.");
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

            outputStream.flush();
            outputStream.close();

            contentValues.clear(); // Clear previous values
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0); // Set to 0 to make it public
            contentResolver.update(newUri, contentValues, null, null);

        } catch (IOException e) {
            e.printStackTrace();

            if (newUri != null) {
                contentResolver.delete(newUri, null, null);
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showFilterSelectionDialog(Context context) {
        filtersDialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.filter_selection_dialog_title)
                .setMultiChoiceItems(R.array.filters_list, selectedFilters, (dialog, which, selected) -> {
                    selectedFilters[which] = selected;
                    updateDialogButtonState(filtersDialog, selectedFilters);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setPositiveButton(R.string.next, (dialog, which) -> {
                    dialog.dismiss();
                    showLanguageSelectionDialog(context);
                })
                .create();

        filtersDialog.setOnShowListener(dialogInterface -> {
            updateDialogButtonState(filtersDialog, selectedFilters);
        });

        filtersDialog.show();
    }

    private void showLanguageSelectionDialog(Context context) {
        languagesDialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.language_selection_dialog_title)
                .setMultiChoiceItems(R.array.languages_list, selectedLanguages, (dialog, which, selected) -> {
                    selectedLanguages[which] = selected;
                    updateDialogButtonState(languagesDialog, selectedLanguages);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNeutralButton(R.string.previous, (dialog, which) -> {
                    dialog.dismiss();
                    showFilterSelectionDialog(context);
                })
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    dialog.dismiss();
                    runBenchmarks(true);
                })
                .create();

        languagesDialog.setOnShowListener(dialogInterface -> {
            updateDialogButtonState(languagesDialog, selectedLanguages);
        });

        languagesDialog.show();
    }

    /**
     * Runs various benchmarks. Can be used to run a single benchmark or multiple ones.
     */
    private void runBenchmarks(boolean multipleFilters) {
        Bitmap original = viewModel.getOriginalBitmap().getValue();
        if (original == null) {
            return;
        }

        List<Filter> allFilters = viewModel.getAllFilters();
        Map<Filter, FilterParams> filterParamsMap = viewModel.getFilterParams().getValue();

        if (!multipleFilters) {
            Boolean shouldUseAssembly = viewModel.shouldUseAssembly().getValue();

            Filter currentFilter = getCurrentFilter();
            if (currentFilter == null) {
                return;
            }
            FilterParams currentFilterParams = getFilterParamsForFilter(currentFilter, filterParamsMap);
            boolean useAssembly = (shouldUseAssembly != null) && shouldUseAssembly;

            executorService.execute(() -> {
                BenchmarkResult result = BenchmarkUtils.runBenchmark(original, currentFilter, currentFilterParams,
                        useAssembly);
                runOnUiThread(() -> viewModel.addOrUpdateBenchmarkResult(result));
            });
        } else {
            if (selectedFilters == null || selectedLanguages == null || allFilters.isEmpty()) {
                return;
            }

            // Create a list to hold results for this batch before updating ViewModel
            List<BenchmarkResult> batchResults = Collections.synchronizedList(new ArrayList<>()); // Use synchronizedList for thread safety

            // Use a CountDownLatch to know when all asynchronous tasks for this batch are complete
            final int[] numTasks = {0}; // Using an array for a mutable int in a lambda
            for (boolean selectedFilter : selectedFilters) {
                if (selectedFilter) {
                    for (boolean selectedLanguage : selectedLanguages) {
                        if (selectedLanguage) {
                            numTasks[0]++; // Increment task counter
                        }
                    }
                }
            }

            if (numTasks[0] == 0) {
                Log.d("Benchmark", "No filters/languages selected for multiple benchmark run.");
                return; // No tasks to run
            }

            // Use a CountDownLatch to wait for all benchmarks in this batch to finish
            CountDownLatch latch = new CountDownLatch(numTasks[0]);

            for (int i = 0; i < selectedFilters.length; i++) {
                if (selectedFilters[i]) {
                    Filter filterToBenchmark = allFilters.get(i + 1);
                    FilterParams currentFilterParams = getFilterParamsForFilter(filterToBenchmark, filterParamsMap);

                    for (int j = 0; j < selectedLanguages.length; j++) {
                        if (selectedLanguages[j]) {
                            boolean shouldUseAssembly = (j == 1);

                            executorService.execute(() -> {
                                BenchmarkResult result = BenchmarkUtils.runBenchmark(original, filterToBenchmark,
                                        currentFilterParams, shouldUseAssembly);
                                batchResults.add(result); // Add to the thread-safe list

                                latch.countDown(); // Decrement latch count

                                if (latch.getCount() == 0) {
                                    // All tasks for this batch are complete
                                    runOnUiThread(() -> {
                                        viewModel.addOrUpdateBenchmarkResults(batchResults);
                                        // Reset selection after batch benchmark
                                        selectedFilters = new boolean[5];
                                        selectedLanguages = new boolean[2];
                                    });
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    private FilterParams getFilterParamsForFilter(Filter filter, Map<Filter, FilterParams> filterParamsMap) {
        return (filterParamsMap != null) ? filterParamsMap.get(filter) : null;
    }

    private Filter getCurrentFilter() {
        Integer position = viewModel.getCurrentFilterPosition().getValue();
        if (position == null || position < 0 || position >= viewModel.getAllFilters().size()) {
            return null;
        }

        List<Filter> allFilters = viewModel.getAllFilters();
        return allFilters.get(position);
    }

    /**
     * Can be used to enable/disable the positive button in a dialog. Primarily used in the filters
     * selection and the languages selection dialog.
     */
    private void updateDialogButtonState(AlertDialog dialog, boolean[] selections) {
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        boolean hasSelection = false;
        for (boolean selected : selections) {
            if (selected) {
                hasSelection = true;
                break;
            }
        }
        positiveButton.setEnabled(hasSelection);
    }

    private static class HorizontalMarginItemDecoration extends RecyclerView.ItemDecoration {

        private final int horizontalMarginInPx;

        HorizontalMarginItemDecoration(Context context, @DimenRes int horizontalMarginInDp) {
            horizontalMarginInPx = (int) context.getResources().getDimension(horizontalMarginInDp);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            outRect.right = horizontalMarginInPx;
            outRect.left = horizontalMarginInPx;
        }
    }
}