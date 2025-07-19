package com.rivan.neon;

/**
 * Data class used to hold the values of a single benchmark.
 */
public class BenchmarkResult {

    /** The name of the filter on which this benchmark was run. */
    private final String filterName;
    /** The language in which this benchmark was run. */
    private final String language;
    /** The average time taken to run this benchmark in milliseconds. */
    private final double averageTimeMs;
    /** Standard deviation of the benchmark runtimes in milliseconds. */
    private final double stdDevMs;
    /** Number of pixels processed per second. */
    private final double pps;

    public BenchmarkResult(String filterName, String language, double averageTimeMs,
                           double stdDevMs, double pps) {
        this.filterName = filterName;
        this.language = language;
        this.averageTimeMs = averageTimeMs;
        this.stdDevMs = stdDevMs;
        this.pps = pps;
    }

    public String getFilterName() { return filterName; }
    public String getLanguage() { return language; }
    public double getAverageTimeMs() { return averageTimeMs; }
    public String getDisplayName() { return filterName + " (" + language + ")"; }
}