package io.github.lino9999.linoac.detection.collector;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;
import java.util.HashMap;

public final class MovementEnvironment {
    private MovementEnvironment() {}

    public static Observation update(Observation input) {
        var values = new HashMap<>(input.values());
        putRaw(values, "protocol-exempt-LAGGED_NEAR_GROUND", both(any(bit(input, "source-cached-solid-footprint"), not(bit(input, "source-lower-footprint-air"))), bit(input, "protocol-fast-move")));
        putRaw(values, "protocol-exempt-LAGGED_NEAR_GROUND_MODERN", both(any(bit(input, "source-three-below-obstructed"), not(bit(input, "source-lower-footprint-air"))), below(input, "protocol-flying-interval-ms", 10)));
        Boolean sharedFlight = any(enums(input, "FLIGHT", "CREATIVE", "JOINED", "LIQUID", "LEVITATION", "SLOW_FALLING", "RIPTIDE", "VEHICLE",
                "LENIENT_SCAFFOLDING", "TELEPORT", "ENDER_PEARL", "CHUNK", "COMBO_MODE", "MYTHIC_MOB", "CLIMBABLE"), bit(input, "source-ceiling-slab-contact"));
        put(values, "step-c", any(bit(input, "source-dripstone-contact"), below(input, "source-crystal-damage-movement-age", 20)));
        put(values, "speed-a", any(enums(input, "FLIGHT", "CREATIVE", "TELEPORT", "LIQUID", "RIPTIDE", "ENDER_PEARL", "VEHICLE"),
                below(input, "source-crystal-damage-movement-age", 3), below(input, "source-velocity-position-age", 3),
                below(input, "source-body-slime-position-age", 15), below(input, "source-explosion-position-age", 5)));
        Boolean jump = any(enums(input, "JOINED", "TELEPORT", "LIQUID", "CLIMBABLE", "GLIDING", "RIPTIDE", "ELYTRA", "ENDER_PEARL", "LEVITATION"),
                both(not(bit(input, "source-last-placement-bridge")), actualEnum(input, "BLOCK_PLACE")), bit(input, "source-candle-contact"),
                below(input, "source-explosion-position-age", 20), below(input, "source-all-damage-age", 20));
        put(values, "jump-c", jump);
        put(values, "jump-a", any(jump, below(input, "source-sulfur-position-age", 40), bit(input, "source-cobweb-contact"), bit(input, "source-heavy-core-contact"), bit(input, "source-amethyst-contact"),
                bit(input, "source-lantern-contact"), bit(input, "source-dried-ghast-contact")));
        Boolean modernBed = input.has("client-at-least-1-9") && input.value("client-at-least-1-9") == 0 ? Boolean.FALSE
                : input.has("client-at-least-1-9") ? below(input, "source-bed-surface-position-age", 40) : null;
        put(values, "jump-b", any(below(input, "source-firework-position-age", 80), below(input, "source-elytra-position-age", 20), modernBed));
        putRaw(values, "source-movement-jump-b-early-exempt", any(below(input, "total-movement-packets", 30), below(input, "source-riptide-position-age", 250),
                below(input, "protocol-gravity", 0), below(input, "source-gravity-change-movement-age", 20)));
        put(values, "flight-a", any(sharedFlight, below(input, "source-velocity-position-age", 20)));
        put(values, "flight-b", any(sharedFlight, bit(input, "source-candle-contact"), bit(input, "source-dried-ghast-contact"),
                below(input, "source-velocity-position-age", 20), bit(input, "source-velocity-pending")));
        put(values, "flight-c", any(sharedFlight, below(input, "source-bed-surface-position-age", 30), below(input, "source-explosion-position-age", 100),
                below(input, "source-slime-below-position-age", 80), below(input, "source-placed-slime-movement-age", 80),
                both(below(input, "source-nonfall-damage-movement-age", 40), below(input, "source-cached-air-position-age", 30)), below(input, "source-velocity-position-age", 40)));
        put(values, "flight-d", any(sharedFlight, below(input, "source-explosion-position-age", 100), below(input, "source-bed-surface-position-age", 30),
                below(input, "source-velocity-position-age", 31)));
        put(values, "flight-e", any(sharedFlight, below(input, "source-explosion-position-age", 20), below(input, "source-velocity-position-age", 20)));
        put(values, "flight-f", any(bit(input, "source-dried-ghast-contact"), below(input, "source-piston-position-age", 20),
                below(input, "source-ceiling-position-age", 20), bit(input, "source-ceiling-slab-contact")));
        Boolean surfaceCached = enums(input, "FLIGHT", "GLIDING", "ELYTRA", "LEVITATION", "VEHICLE", "RIPTIDE", "TELEPORT");
        Boolean surfaceMaterials = any(bit(input, "source-heavy-core-contact"), bit(input, "source-repeater-contact"), bit(input, "source-dripleaf-contact"));
        put(values, "jesus-a", any(enums(input, "FLIGHT", "GLIDING", "LEVITATION", "VEHICLE", "RIPTIDE", "TELEPORT", "CHUNK"),
                surfaceMaterials, bit(input, "source-candle-contact"), below(input, "source-nearby-boat-position-age", 20)));
        put(values, "jesus-b", any(surfaceCached, surfaceMaterials, below(input, "source-nearby-boat-position-age", 20)));
        put(values, "jesus-c", any(surfaceCached, surfaceMaterials, bit(input, "source-cobweb-contact"), bit(input, "source-candle-contact")));
        put(values, "jesus-d", any(surfaceCached, surfaceMaterials));
        put(values, "jesus-e", any(surfaceCached, surfaceMaterials, bit(input, "source-candle-contact")));
        put(values, "antilevitation-a", enums(input, "TELEPORT", "CLIMBABLE", "FLIGHT", "CREATIVE", "LIQUID", "ELYTRA"));
        putRaw(values, "source-budget-jesus-b", input.has("source-solid-support-position-age")
                ? .11 + (input.value("source-solid-support-position-age") < 10 ? .05 : 0) : null);
        if (input.hasAll("source-solid-support-position-age", "source-swimming-contact-position-age", "source-nearby-living-position-age", "source-walk-speed", "source-speed-amplifier")) {
            double limit = threshold(input, .166f);
            if (input.value("source-solid-support-position-age") < 10 || input.value("source-swimming-contact-position-age") < 20) limit += .35;
            if (input.value("source-nearby-living-position-age") < 20) limit += .2;
            values.put("source-budget-jesus-e", limit);
        } else values.remove("source-budget-jesus-e");
        return new Observation(input.kind(), input.tick(), input.nanos(), values, input.states());
    }

    private static Boolean enums(Observation input, String... names) {
        Boolean[] values = new Boolean[names.length];
        for (int i = 0; i < names.length; i++) values[i] = bit(input, CachedExemptionState.key(names[i]));
        return any(values);
    }
    private static Boolean actualEnum(Observation input, String name) {
        var result = ProtocolExemptions.evaluate(input, name);
        return result.active() ? Boolean.TRUE : result.known() ? Boolean.FALSE : null;
    }
    private static Boolean not(Boolean value) { return value == null ? null : !value; }
    private static Boolean bit(Observation input, String key) {
        if (!input.has(key)) return null;
        double value = input.value(key);
        return value == 0 ? Boolean.FALSE : value == 1 ? Boolean.TRUE : null;
    }
    private static Boolean below(Observation input, String key, double limit) {
        return input.has(key) && Double.isFinite(input.value(key)) ? input.value(key) < limit : null;
    }
    private static Boolean both(Boolean first, Boolean second) {
        return Boolean.FALSE.equals(first) || Boolean.FALSE.equals(second) ? Boolean.FALSE : first != null && second != null ? Boolean.TRUE : null;
    }
    private static Boolean any(Boolean... values) {
        boolean known = true;
        for (Boolean value : values) { if (Boolean.TRUE.equals(value)) return true; if (value == null) known = false; }
        return known ? false : null;
    }
    private static void put(java.util.Map<String, Double> values, String id, Boolean active) { putRaw(values, "source-movement-" + id + "-private-exempt", active); }
    private static void putRaw(java.util.Map<String, Double> values, String key, Boolean active) { if (active == null) values.remove(key); else values.put(key, active ? 1d : 0d); }
    private static void putRaw(java.util.Map<String, Double> values, String key, Double value) { if (value == null) values.remove(key); else values.put(key, value); }
    private static float threshold(Observation input, float base) {
        float speed = (float) input.value("source-speed-amplifier");
        float candidate = base + speed * .0675f + ((float) input.value("source-walk-speed") - .2f) * 3.5f;
        return candidate < base ? base + speed * .0675f : base;
    }
}
