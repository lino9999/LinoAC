package io.github.lino9999.linoac.packet;

import io.github.lino9999.linoac.detection.Observation;
import java.util.Map;
import java.util.Set;

public record WirePacket(Observation.Kind kind, String type, long nanos, long token,
                         Map<String, Double> values, Set<String> states) {
    public WirePacket {
        values = Map.copyOf(values);
        states = Set.copyOf(states);
    }
    public boolean state(String state) { return states.contains(state); }
}
