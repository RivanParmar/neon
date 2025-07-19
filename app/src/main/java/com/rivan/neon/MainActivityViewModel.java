package com.rivan.neon;

import static com.rivan.neon.FilterProcessor.applyFilterToBitmap;

import android.app.Application;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivityViewModel extends AndroidViewModel {

    private static final String TAG = "MainActivityViewModel";

    private final MutableLiveData<Bitmap> _originalBitmap = new MutableLiveData<>();
    public LiveData<Bitmap> getOriginalBitmap() {
        return _originalBitmap;
    }

    private final MutableLiveData<List<FilterPreviewItem>> _allPreviews = new MutableLiveData<>();
    public LiveData<List<FilterPreviewItem>> getAllPreviews() {
        return _allPreviews;
    }

    private final MutableLiveData<Integer> _currentFilterPosition = new MutableLiveData<>();
    public LiveData<Integer> getCurrentFilterPosition() {
        return _currentFilterPosition;
    }

    private final MutableLiveData<Boolean> _shouldUseAssembly = new MutableLiveData<>(false);
    public void setUseAssembly(boolean shouldUseAssembly) {
        if (_shouldUseAssembly.getValue() != shouldUseAssembly) {
            _shouldUseAssembly.setValue(shouldUseAssembly);
        }
    }
    public LiveData<Boolean> shouldUseAssembly() {
        return _shouldUseAssembly;
    }

    private final MutableLiveData<Map<Filter, FilterParams>> _filterParams = new MutableLiveData<>();
    public LiveData<Map<Filter, FilterParams>> getFilterParams() {
        return _filterParams;
    }

    private final MutableLiveData<List<BenchmarkResult>> _benchmarkResults = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<BenchmarkResult>> getBenchmarkResults() {
        return _benchmarkResults;
    }

    /**
     * Adds or updates a single benchmark result in the list.
     * If a result for the same filter and language already exists, it's updated.
     * Otherwise, the new result is added.
     *
     * @param newResult The new or updated BenchmarkResult.
     */
    public void addOrUpdateBenchmarkResult(BenchmarkResult newResult) {
        List<BenchmarkResult> currentResults = _benchmarkResults.getValue();
        if (currentResults == null) {
            currentResults = new ArrayList<>();
        } else {
            // Create a mutable copy if the LiveData holds an immutable list
            currentResults = new ArrayList<>(currentResults);
        }

        boolean updatedExisting = false;
        for (int i = 0; i < currentResults.size(); i++) {
            BenchmarkResult existingResult = currentResults.get(i);
            // Check if it's the same filter and language
            if (Objects.equals(existingResult.getFilterName(), newResult.getFilterName()) &&
                    Objects.equals(existingResult.getLanguage(), newResult.getLanguage())) {
                currentResults.set(i, newResult); // Update the existing result
                updatedExisting = true;
                break;
            }
        }

        if (!updatedExisting) {
            currentResults.add(newResult); // Add new result if no existing one was found
        }

        _benchmarkResults.setValue(currentResults); // Post the updated list
    }

    /**
     * Adds or updates a list of benchmark results. Existing results for the same
     * filter/language combinations will be updated; new ones will be added.
     * Results not present in the new batch will remain unchanged.
     *
     * @param newResultsList The list of new or updated BenchmarkResults.
     */
    public void addOrUpdateBenchmarkResults(List<BenchmarkResult> newResultsList) {
        if (newResultsList == null || newResultsList.isEmpty()) {
            return; // Nothing to add or update
        }

        List<BenchmarkResult> currentResults = _benchmarkResults.getValue();
        if (currentResults == null) {
            currentResults = new ArrayList<>();
        } else {
            currentResults = new ArrayList<>(currentResults); // Create a mutable copy
        }

        // Use a map for efficient lookup of existing results by a unique key (filterName + language)
        Map<String, BenchmarkResult> existingResultsMap = new HashMap<>();
        for (BenchmarkResult result : currentResults) {
            String key = result.getFilterName() + "_" + result.getLanguage();
            existingResultsMap.put(key, result);
        }

        // Iterate through the new results and update/add to the map
        for (BenchmarkResult newResult : newResultsList) {
            String key = newResult.getFilterName() + "_" + newResult.getLanguage();
            existingResultsMap.put(key, newResult); // This will overwrite existing or add new
        }

        // Convert the map values back to a list and update LiveData
        _benchmarkResults.setValue(new ArrayList<>(existingResultsMap.values()));
    }


    /**
     * Overwrites all benchmark results with a new list.
     * @param benchmarkResults The new list of benchmark results.
     */
    public void setAllBenchmarkResults(List<BenchmarkResult> benchmarkResults) {
        _benchmarkResults.setValue(new ArrayList<>(benchmarkResults)); // Always set a new mutable list
    }

    /**
     * Clears all benchmark results.
     */
    public void clearAllBenchmarkResults() {
        _benchmarkResults.setValue(new ArrayList<>());
    }

    private final ExecutorService executorService;
    private final Handler mainHandler;

    private final List<Filter> allFilters = Arrays.asList(Filter.values());

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        initializeFilterParams();
    }

    /**
     * Asynchronously loads an image from the given URI, decodes it to a sampled bitmap
     * to fit specified dimensions, and then updates the LiveData.
     * Upon successful loading, it also initializes filter previews and starts their sequential update.
     *
     * @param imageUri       The {@link Uri} of the image to load.
     * @param requiredWidth  The desired width for the sampled bitmap.
     * @param requiredHeight The desired height for the sampled bitmap.
     */
    public void loadImageFromUri(Uri imageUri, int requiredWidth, int requiredHeight) {
        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                bitmap = decodeSampledBitmapFromUri(getApplication().getContentResolver(),
                        imageUri, requiredWidth, requiredHeight);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                if (finalBitmap != null) {
                    _originalBitmap.setValue(finalBitmap);
                    initializeAllPreviewsWithOriginal(finalBitmap);
                    startSequentialPreviewUpdate();
                } else {
                    _originalBitmap.setValue(null);
                    _allPreviews.setValue(new ArrayList<>());
                    Log.e(TAG, "Failed to load bitmap from URI.");
                }
            });
        });
    }

    /**
     * Updates the parameters for a specific filter. This will trigger a re-application
     * of the filter to its preview image.
     *
     * @param filter          The {@link Filter} whose parameters are being updated.
     * @param newFilterParams The new {@link FilterParams} for the specified filter.
     */
    public void updateFilterParams(Filter filter, FilterParams newFilterParams) {
        Map<Filter, FilterParams> currentParams = _filterParams.getValue();
        if (currentParams == null) {
            currentParams = new HashMap<>();
        }

        currentParams.put(filter, newFilterParams.copy());
        _filterParams.setValue(currentParams);

        int filterIndex = allFilters.indexOf(filter);
        if (filterIndex != -1) {
            updateFilterPreview(filterIndex);
        }
    }

    /**
     * Initializes the {@link LiveData} containing all filter preview items.
     * Each item is initially set with a copy of the original bitmap and a loading state.
     *
     * @param originalBitmap The original {@link Bitmap} from which previews are derived.
     */
    private void initializeAllPreviewsWithOriginal(Bitmap originalBitmap) {
        if (originalBitmap == null) {
            _allPreviews.setValue(new ArrayList<>());
            return;
        }

        List<FilterPreviewItem> initialPreviewItems = new ArrayList<>();
        for (int i = 0; i < allFilters.size(); i++) {
            Filter filter = allFilters.get(i);
            boolean isLoading = (filter != Filter.ORIGINAL);
            initialPreviewItems.add(new FilterPreviewItem(filter, originalBitmap.copy(Bitmap.Config.ARGB_8888, true), isLoading));
        }
        _allPreviews.setValue(initialPreviewItems);
    }

    /**
     * Starts the asynchronous process of applying each filter to the original bitmap
     * and updating its corresponding preview item sequentially. This method is called
     * after the original image is loaded or when all filters need to be re-applied.
     */
    private void startSequentialPreviewUpdate() {
        Bitmap original = _originalBitmap.getValue();
        if (original == null) {
            return;
        }

        Map<Filter, FilterParams> currentParams = _filterParams.getValue();
        if (currentParams == null) {
            Log.e(TAG, "Filter parameters not initialized.");
            return;
        }

        List<FilterPreviewItem> currentPreviews = _allPreviews.getValue();
        if (currentPreviews == null || currentPreviews.isEmpty()) {
            Log.e(TAG, "Preview items list is empty during sequential update.");
            return;
        }

        for (int i = 0; i < allFilters.size(); i++) {
            final int index = i;
            final Filter filter = allFilters.get(i);
            if (filter == Filter.ORIGINAL) {
                continue;
            }

            final FilterParams params = currentParams.get(filter);

            executorService.execute(() -> {
                Bitmap finalFilteredPreview = applyFilterToBitmap(original, filter, params,
                        _shouldUseAssembly.getValue() != null && _shouldUseAssembly.getValue());
                mainHandler.post(() -> updateFilterPreviewItems(index, finalFilteredPreview, filter));
            });
        }
    }

    /**
     * Updates the preview image for a specific filter at a given index. This is typically
     * called when a filter's parameters are changed, or when the underlying implementation
     * (Java/Assembly) preference is toggled.
     *
     * @param filterIndex The index of the filter in the {@code allFilters} list whose preview
     *                    needs to be updated.
     */
    public void updateFilterPreview(int filterIndex) {
        if (filterIndex < 0 || filterIndex >= allFilters.size()) {
            return;
        }

        Bitmap original = _originalBitmap.getValue();
        if (original == null) {
            return;
        }

        final Filter filterToApply = allFilters.get(filterIndex);
        final FilterParams params = _filterParams.getValue() != null ?
                _filterParams.getValue().get(filterToApply) : null;

        List<FilterPreviewItem> currentPreviews = _allPreviews.getValue();
        if (currentPreviews == null || filterIndex >= currentPreviews.size()) {
            return;
        }

        // Set item to loading state before starting background task
        List<FilterPreviewItem> loadingPreviews = new ArrayList<>(currentPreviews);
        FilterPreviewItem oldItemForLoading = loadingPreviews.get(filterIndex);
        FilterPreviewItem newItemForLoading = new FilterPreviewItem(oldItemForLoading.getFilter(), oldItemForLoading.getPreviewBitmap(), true); // Now loading
        loadingPreviews.set(filterIndex, newItemForLoading); // Replace with new instance
        _allPreviews.setValue(loadingPreviews); // Trigger UI to show loading indicator

        executorService.execute(() -> {
            Bitmap finalUpdatedPreview = applyFilterToBitmap(original, filterToApply, params,
                    _shouldUseAssembly.getValue() != null && _shouldUseAssembly.getValue());
            mainHandler.post(() -> updateFilterPreviewItems(filterIndex, finalUpdatedPreview, filterToApply));
        });
    }

    private void updateFilterPreviewItems(int filterIndex, Bitmap finalUpdatedPreview,
                                          Filter filterToApply) {
        List<FilterPreviewItem> postProcessPreviews = _allPreviews.getValue();
        if (postProcessPreviews != null && filterIndex < postProcessPreviews.size()) {
            // Create a new list copy to ensure LiveData observer is triggered
            List<FilterPreviewItem> updatedList = new ArrayList<>(postProcessPreviews);
            FilterPreviewItem oldItem = updatedList.get(filterIndex);
            FilterPreviewItem newItem;
            if (finalUpdatedPreview != null) {
                newItem = new FilterPreviewItem(oldItem.getFilter(), finalUpdatedPreview, false); // Not loading
            } else {
                Log.e(TAG, "Failed to update filter for " + filterToApply.name() + " at index " + filterIndex + ". Keeping old bitmap or setting placeholder.");
                newItem = new FilterPreviewItem(oldItem.getFilter(), oldItem.getPreviewBitmap(), false); // Stop loading even on error
            }
            updatedList.set(filterIndex, newItem);
            _allPreviews.setValue(updatedList);
        }
    }

    public void reapplyAllFilters() {
        Bitmap original = _originalBitmap.getValue();
        if (original == null) {
            return;
        }

        List<FilterPreviewItem> resetPreviews = new ArrayList<>();
        for (int i = 0; i < allFilters.size(); i++) {
            Filter filter = allFilters.get(i);
            boolean isLoading = (filter != Filter.ORIGINAL);
            resetPreviews.add(new FilterPreviewItem(filter, original.copy(Bitmap.Config.ARGB_8888, true), isLoading));
        }
        _allPreviews.setValue(resetPreviews);
        startSequentialPreviewUpdate();
    }

    /**
     * Sets the currently selected filter's position in the UI.
     * This method prevents unnecessary LiveData updates if the position is already the same.
     *
     * @param position The integer position of the selected filter (corresponds to
     *                 {@code allFilters} index).
     */
    public void setCurrentFilter(int position) {
        if (_currentFilterPosition.getValue() == null || _currentFilterPosition.getValue() != position) {
            _currentFilterPosition.setValue(position);
        }
    }

    public List<Filter> getAllFilters() {
        return allFilters;
    }

    /**
     * Initializes the default {@link FilterParams} for all filters that require them.
     * This method is called once during ViewModel creation to set up initial parameter states.
     */
    private void initializeFilterParams() {
        Map<Filter, FilterParams> initialParams = new HashMap<>();
        for (Filter filter : allFilters) {
            FilterParams params = filter.createDefaultParams();
            if (params != null) {
                initialParams.put(filter, params);
            }
        }
        _filterParams.setValue(initialParams);
    }

    /**
     * Decodes a sampled {@link Bitmap} from a given {@link Uri}, scaling it down
     * to prevent out-of-memory errors and optimize performance for UI display.
     *
     * <p>
     * See: <a href="https://developer.android.com/topic/performance/graphics/load-bitmap">
     *     Loading Large Bitmaps Efficiently</a>
     * </p>
     *
     * @param contentResolver The {@link ContentResolver} to open the URI.
     * @param imageUri        The {@link Uri} of the image to decode.
     * @param requiredWidth   The desired width for the resulting bitmap.
     * @param requiredHeight  The desired height for the resulting bitmap.
     *
     * @return The decoded and sampled {@link Bitmap}, or {@code null} if decoding fails.
     */
    private Bitmap decodeSampledBitmapFromUri(ContentResolver contentResolver, Uri imageUri,
                                              int requiredWidth, int requiredHeight) {
        InputStream inputStream = null;
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            // First pass: Just decode bounds to get original dimensions
            options.inJustDecodeBounds = true;
            inputStream = contentResolver.openInputStream(imageUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            closeQuietly(inputStream); // Close stream after first pass

            // Calculate the optimal inSampleSize
            options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);

            // Second pass: Decode the actual bitmap with the calculated inSampleSize
            options.inJustDecodeBounds = false; // Set to false to decode full bitmap
            inputStream = contentResolver.openInputStream(imageUri);
            return BitmapFactory.decodeStream(inputStream, null, options);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            closeQuietly(inputStream);
        }
    }

    /**
     * Calculates an appropriate {@code inSampleSize} value for {@link BitmapFactory.Options}.
     * This value determines how much to downsample an image to efficiently load it
     * into memory while meeting a required target size.
     *
     * <p>
     * See: <a href="https://developer.android.com/topic/performance/graphics/load-bitmap">
     *     Loading Large Bitmaps Efficiently</a>
     * </p>
     *
     * @param options        The {@link BitmapFactory.Options} containing the original image dimensions.
     * @param requiredWidth  The target width for the bitmap.
     * @param requiredHeight The target height for the bitmap.
     *
     * @return The calculated {@code inSampleSize} (a power of 2).
     */
    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int requiredWidth, int requiredHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > requiredHeight || width > requiredWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= requiredHeight
                    && (halfWidth / inSampleSize) >= requiredWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void closeQuietly(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) { /* Ignore */ }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
