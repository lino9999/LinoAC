package io.github.lino9999.linoac.detection;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public final class DetectionMemory {
    private final Map<String, Double> values = new HashMap<>();
    private final Map<String, ArrayDeque<Double>> windows = new HashMap<>();

    public double get(String key) { return values.getOrDefault(key, 0d); }
    public double get(String key, double fallback) { return values.getOrDefault(key, fallback); }
    public void set(String key, double value) { values.put(key, value); }
    public double increment(String key) { return increment(key, 1); }
    public double increment(String key, double amount) {
        double value = get(key) + amount;
        set(key, value);
        return value;
    }
    public double[] window(String key, double value, int capacity) {
        if (capacity < 1 || capacity > 512) throw new IllegalArgumentException("Invalid sample capacity");
        ArrayDeque<Double> samples = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        if (samples.size() >= capacity) samples.removeFirst();
        samples.addLast(value);
        return samples.stream().mapToDouble(Double::doubleValue).toArray();
    }
    public void clear() { values.clear(); windows.clear(); }
}
