package io.github.lino9999.linoac.detection;

import java.util.Arrays;

public final class Statistics {
    private Statistics() {}

    public static double mean(double[] samples) { return Arrays.stream(samples).average().orElse(0); }
    public static double variance(double[] samples) {
        double mean = mean(samples);
        return Arrays.stream(samples).map(value -> (value - mean) * (value - mean)).average().orElse(0);
    }
    public static double deviation(double[] samples) { return Math.sqrt(variance(samples)); }
    public static double skewness(double[] samples) {
        double mean = mean(samples), deviation = deviation(samples);
        return deviation == 0 ? 0 : Arrays.stream(samples).map(value -> Math.pow((value - mean) / deviation, 3)).average().orElse(0);
    }
    public static double kurtosis(double[] samples) {
        double mean = mean(samples), variance = variance(samples);
        return variance == 0 ? 0 : Arrays.stream(samples).map(value -> Math.pow(value - mean, 4)).average().orElse(0) / (variance * variance) - 3;
    }
    public static double median(double[] samples) {
        if (samples.length == 0) return 0;
        double[] sorted = samples.clone(); Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2 : sorted[middle];
    }
    public static double range(double[] samples) {
        return Arrays.stream(samples).max().orElse(0) - Arrays.stream(samples).min().orElse(0);
    }
    public static long distinct(double[] samples) { return Arrays.stream(samples).distinct().count(); }
    public static double gcd(double first, double second) {
        long a = Math.round(Math.abs(first) * (1L << 24));
        long b = Math.round(Math.abs(second) * (1L << 24));
        while (b > 16) { long next = a % b; a = b; b = next; }
        return a / (double) (1L << 24);
    }
    public static double wrappedDegrees(double degrees) {
        double result = degrees % 360;
        return result >= 180 ? result - 360 : result < -180 ? result + 360 : result;
    }
}
