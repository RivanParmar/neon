package com.rivan.neon;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkUtils {

    public static BenchmarkResult runBenchmark(Bitmap originalBitmap, Filter filter,
                                    @Nullable FilterParams params, boolean useAssembly) {
        if (originalBitmap == null || filter == null || filter == Filter.ORIGINAL) {
            return null;
        }

        List<Long> measurementTimesNs = new ArrayList<>();

        try {
            for (int i = 0; i < 10; i++) {
                Bitmap bitmapForWarmup = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Bitmap result = FilterProcessor.applyFilterToBitmap(bitmapForWarmup, filter, params, useAssembly);
                
                if (result == null || result.isRecycled()) {
                    return null;
                }

                if (bitmapForWarmup != originalBitmap && !bitmapForWarmup.isRecycled()) {
                    bitmapForWarmup.recycle();
                }
            }

            for (int i = 0; i < 50; i++) {
                Bitmap bitmapForMeasurement = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

                long measuredDurationMs = FilterProcessor.measureFilterTime(
                        bitmapForMeasurement, filter, params, useAssembly);

                measurementTimesNs.add(measuredDurationMs);

                if (bitmapForMeasurement != originalBitmap && !bitmapForMeasurement.isRecycled()) {
                    bitmapForMeasurement.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        long totalTimeNs = 0;
        for (long time: measurementTimesNs) {
            totalTimeNs += time;
        }
        double averageTimeNs = (double) totalTimeNs / measurementTimesNs.size();
        double averageTimeMs = averageTimeNs / 1_000_000.0;

        double sumOfSquaredDiffs = 0;
        for (Long time : measurementTimesNs) {
            sumOfSquaredDiffs += Math.pow(time - averageTimeNs, 2);
        }
        double stdDevNs = Math.sqrt(sumOfSquaredDiffs / measurementTimesNs.size());
        double stdDevMs = stdDevNs / 1_000_000.0;

        long totalPixels = (long) originalBitmap.getWidth() * originalBitmap.getHeight();
        double pps = (averageTimeNs > 0) ? (totalPixels / (averageTimeNs / 1_000_000_000.0)) : 0;

        Log.d("NBench", String.format("Average Time: %.2f ms (StdDev: %.2f ms)", averageTimeMs, stdDevMs));
        Log.d("NBench", String.format("Pixels Per Second (PPS): %.2f", pps));
        Log.d("NBench", "--- Benchmarking Complete for " + filter.name() + " ---");

        return new BenchmarkResult(filter.name(), useAssembly ? "Assembly" : "Java", averageTimeMs, stdDevMs, pps);
    }
}
