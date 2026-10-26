package io.github.lino9999.linoac.detection;

import java.util.Map;
import java.util.Set;

public record Observation(Kind kind, long tick, long nanos, Map<String, Double> values, Set<String> states) {
    public Observation {
        values = Map.copyOf(values);
        states = Set.copyOf(states);
    }

    public double value(String key) { return values.getOrDefault(key, 0d); }
    public double value(String key, double fallback) { return values.getOrDefault(key, fallback); }
    public boolean has(String key) { return values.containsKey(key); }
    public boolean state(String key) { return states.contains(key); }
    public boolean hasAll(String... keys) {
        for (String key : keys) if (!has(key)) return false;
        return true;
    }

    public enum Kind { MOVE, ROTATION, ATTACK, SWING, PLACE, BREAK, DIG, INVENTORY, USE, RELEASE, PACKET, VELOCITY, SERVER_POSITION, ACKNOWLEDGEMENT, TICK }
}
