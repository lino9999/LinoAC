package io.github.lino9999.linoac.detection.collector;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CachedExemptionState {
    private static final List<String> TYPES = List.of("FLIGHT", "CREATIVE", "JOINED", "LIQUID", "LEVITATION", "SLOW_FALLING", "RIPTIDE", "VEHICLE",
            "LENIENT_SCAFFOLDING", "GLIDING", "ELYTRA", "TELEPORT", "ENDER_PEARL", "CHUNK", "COMBO_MODE", "MYTHIC_MOB", "CLIMBABLE");
    private final Map<String, Double> cached = new HashMap<>();

    public CachedExemptionState() { for (String type : TYPES) cached.put(key(type), 0d); }
    public void position(Observation beforeFlyingClocks) {
        for (String type : TYPES) {
            Boolean active = ProtocolExemptions.value(beforeFlyingClocks, type);
            if (active == null) cached.remove(key(type)); else cached.put(key(type), active ? 1d : 0d);
        }
    }
    public void values(Map<String, Double> destination) { for (String type : TYPES) destination.remove(key(type)); destination.putAll(cached); }
    public static String key(String type) { return "source-cached-exempt-" + type; }
}
