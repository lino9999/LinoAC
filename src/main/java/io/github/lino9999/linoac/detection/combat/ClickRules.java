package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import io.github.lino9999.linoac.detection.Statistics;

import java.util.Arrays;
import java.util.Set;

final class ClickRules implements RuleModule {
    private static final Set<String> IDS = Set.of("autoclicker-a", "autoclicker-b", "autoclicker-c", "autoclicker-d", "autoclicker-e", "autoclicker-f", "autoclicker-g", "autoclicker-h", "autoclicker-i", "autoclicker-j", "autoclicker-k", "autoclicker-l", "autoclicker-m", "autoclicker-n", "autoclicker-o", "autoclicker-p", "autoclicker-q", "autoclicker-r", "autoclicker-s", "autoclicker-t");
    private static final int[] CAPACITIES = {20, 50, 20, 50, 50, 50, 50, 20, 50, 10, 50, 50, 50, 25, 10, 15, 40, 30, 25, 25};

    public Set<String> checkIds() { return IDS; }

    public void evaluate(Observation observation, DetectionMemory memory, FindingSink sink) {
        if (observation.kind() != Observation.Kind.SWING || !observation.state("click-context-known")
                || !observation.hasAll("click-time-ms", "click-delay-ms") || observation.state("click-non-dig-exempt")) return;
        double now = observation.value("click-time-ms");
        double delay = observation.value("click-delay-ms");
        if (!Double.isFinite(now) || now < 0 || !Double.isFinite(delay) || delay < 0) return;
        double previous = memory.get("combat:click:evaluation-time", -1);
        if (previous >= 0 && now < previous) {
            for (char variant = 'a'; variant <= 't'; variant++) memory.set(countKey(variant), 0);
            memory.set("combat:click:evaluation-time", now);
            memory.set("combat:click:a-time", now);
            return;
        }
        memory.set("combat:click:evaluation-time", now);
        double previousLimit = memory.get("combat:click:a-time");
        memory.set("combat:click:a-time", now);
        double[] limit = append(memory, 'a', now - previousLimit);
        if (limit != null) {
            double cps = ClickStatistics.cps(limit);
            if (cps > observation.value("cps-limit", 25) && cps > 5) sink.direct("autoclicker-a", "average-cps=" + cps);
        }
        if (observation.state("click-statistical-exempt")) return;
        if (delay > 5000) {
            for (char variant = 'b'; variant <= 't'; variant++) memory.set(countKey(variant), 0);
            return;
        }
        for (char variant = 'b'; variant <= 't'; variant++) {
            double[] samples = append(memory, variant, delay);
            if (samples != null) evaluate(variant, samples, observation, memory, sink);
        }
    }

    private void evaluate(char variant, double[] samples, Observation observation, DetectionMemory memory, FindingSink sink) {
        String id = "autoclicker-" + variant;
        switch (variant) {
            case 'b' -> sink.sampleAbove(id, ClickStatistics.dispersion(samples) < 167, "dispersion-ms=" + ClickStatistics.dispersion(samples));
            case 'c' -> {
                double cps = ClickStatistics.cps(samples);
                double error = Math.abs(Math.round(cps) - cps);
                sink.sampleAbove(id, error < .08, "cps-rounding-error=" + error);
            }
            case 'd' -> {
                double bias = ClickStatistics.medianBias(samples);
                sink.sampleAbove(id, bias < -.01, "median-bias=" + bias);
            }
            case 'e' -> {
                double spread = ClickStatistics.scatter(samples) / 1000;
                sink.sampleAbove(id, spread < 28.2, "scaled-scatter=" + spread);
            }
            case 'f' -> {
                long distinct = Statistics.distinct(samples);
                sink.sampleAbove(id, distinct < 13, "distinct-delays=" + distinct);
            }
            case 'g' -> {
                long outliers = Arrays.stream(samples).filter(value -> value > 150).count();
                sink.sampleAbove(id, outliers < 3, "long-delays=" + outliers);
            }
            case 'h' -> difference(memory, sink, id, ClickStatistics.dispersion(samples), 7.52, "dispersion-change-ms");
            case 'i' -> {
                double weight = ClickStatistics.tailWeight(samples) / 1000;
                sink.sampleAbove(id, weight < 41.78, "scaled-tail-weight=" + weight);
            }
            case 'j' -> sink.sampleAbove(id, Statistics.range(samples) < 50, "delay-range-ms=" + Statistics.range(samples));
            case 'k' -> difference(memory, sink, id, Statistics.mean(samples), 2.56, "mean-change-ms");
            case 'l' -> difference(memory, sink, id, ClickStatistics.tailWeight(samples) / 1000, 8.35, "scaled-tail-weight-change");
            case 'm' -> difference(memory, sink, id, ClickStatistics.scatter(samples) / 1000, 5.28, "scaled-scatter-change");
            case 'n' -> rollingDifference(memory, sink, id, ClickStatistics.dispersion(samples), .25);
            case 'o' -> {
                String key = "combat:click:o-previous";
                double cps = ClickStatistics.cps(samples);
                double previous = memory.get(key, -1);
                if (previous > 0) {
                    double mean = (cps + previous) / 2;
                    double difference = Math.abs(cps - previous);
                    sink.sampleAbove(id, mean > 9.25 && difference > 2.8, "mean-cps=" + mean + " cps-change=" + difference);
                }
                memory.set(key, cps);
            }
            case 'p' -> {
                double dispersion = ClickStatistics.dispersion(samples);
                double bias = ClickStatistics.medianBias(samples);
                double weight = ClickStatistics.tailWeight(samples);
                boolean repeated = dispersion == memory.get("combat:click:p-dispersion")
                        && bias == memory.get("combat:click:p-bias") && weight == memory.get("combat:click:p-weight");
                if (repeated) sink.sampleAbove(id, true, "Repeated dispersion, median bias and tail weight");
                else sink.reset(id, "Statistical batch changed");
                memory.set("combat:click:p-dispersion", dispersion);
                memory.set("combat:click:p-bias", bias);
                memory.set("combat:click:p-weight", weight);
            }
            case 'q' -> rollingDifference(memory, sink, id, ClickStatistics.dispersion(samples), 3);
            case 'r' -> {
                long outliers = Arrays.stream(samples).filter(value -> value > 150).count();
                sink.sampleAbove(id, outliers == 0, "long-delays=" + outliers);
            }
            case 's' -> {
                long distinct = Statistics.distinct(samples);
                sink.sampleAbove(id, distinct < 6, "distinct-delays=" + distinct);
            }
            case 't' -> {
                double weight = ClickStatistics.tailWeight(samples) / 1000;
                memory.set("combat:click:tail-weight", weight);
                sink.sampleAbove(id, weight < observation.value("click-tail-weight-minimum", 13), "scaled-tail-weight=" + weight);
            }
            default -> throw new IllegalArgumentException("Unknown click variant");
        }
    }

    private void difference(DetectionMemory memory, FindingSink sink, String id, double current, double threshold, String detail) {
        String key = "combat:click:previous:" + id;
        double difference = Math.abs(current - memory.get(key));
        sink.sampleAbove(id, difference < threshold, detail + "=" + difference);
        memory.set(key, current);
    }

    private void rollingDifference(DetectionMemory memory, FindingSink sink, String id, double current, double threshold) {
        String key = "combat:click:previous:" + id;
        double previous = memory.get(key);
        double difference = Math.abs(current - previous);
        double mean = Math.abs(current + previous) / 2;
        sink.sampleAbove(id, difference < threshold && mean < 150, "dispersion-change-ms=" + difference + " mean-dispersion-ms=" + mean);
        memory.set(key, current);
    }

    private double[] append(DetectionMemory memory, char variant, double delay) {
        int capacity = CAPACITIES[variant - 'a'];
        double[] samples = memory.window("combat:click:window:" + variant, delay, capacity);
        double count = Math.min(capacity, memory.increment(countKey(variant)));
        memory.set(countKey(variant), count);
        if (count < capacity) return null;
        if (variant != 'n' && variant != 'q' && variant != 'r' && variant != 's' && variant != 't') memory.set(countKey(variant), 0);
        return samples;
    }

    private static String countKey(char variant) { return "combat:click:count:" + variant; }
}
