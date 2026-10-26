package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.Statistics;

final class ClickStatistics {
    private ClickStatistics() {}

    static double cps(double[] values) { return 20 / Statistics.mean(values) * 50; }

    static double scatter(double[] values) {
        double mean = Statistics.mean(values);
        double sum = 0;
        for (double value : values) {
            double difference = value - mean;
            sum += Math.pow(difference, 2);
        }
        return sum;
    }

    static double dispersion(double[] values) { return Math.sqrt(scatter(values)); }

    static double medianBias(double[] values) {
        double scatter = scatter(values);
        return scatter == 0 ? Double.NaN : 3 * (Statistics.mean(values) - Statistics.median(values)) / scatter;
    }

    static double tailWeight(double[] values) {
        int size = values.length;
        if (size < 4) return Double.NaN;
        double total = 0;
        for (double value : values) total += value;
        double scatter = scatter(values);
        if (total <= 0 || scatter == 0) return Double.NaN;
        double mean = total / size;
        double fourth = 0;
        for (double value : values) fourth += Math.pow(mean - value, 4);
        double coefficient = size * (size + 1d) / ((size - 1d) * (size - 2d) * (size - 3d));
        double correction = 3 * Math.pow(size - 1d, 2) / ((size - 2d) * (size - 3d));
        return coefficient * (fourth / Math.pow(scatter / total, 2)) - correction;
    }
}
