package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.Observation;

public final class ProtocolExemptions {
    private ProtocolExemptions() {}

    public static boolean any(Observation observation, String... exemptions) {
        for (String exemption : exemptions) if (Boolean.TRUE.equals(value(observation, exemption))) return true;
        return false;
    }

    public static boolean known(Observation observation, String... exemptions) {
        for (String exemption : exemptions) if (value(observation, exemption) == null) return false;
        return true;
    }

    public static Boolean value(Observation o, String name) {
        String key = "protocol-exempt-" + name;
        if (o.has(key)) return o.value(key) == 0 ? Boolean.FALSE : o.value(key) == 1 ? Boolean.TRUE : null;
        return switch (name) {
            case "SERVER_VERSION" -> binary(o, "protocol-server-at-least-1-9");
            case "CLIENT_VERSION" -> binary(o, "client-at-least-1-9");
            case "FAST" -> binary(o, "protocol-fast-move");
            case "CREATIVE" -> either(binary(o, "protocol-bukkit-creative"), binary(o, "protocol-cached-creative"));
            case "SPECTATOR" -> binary(o, "protocol-bukkit-spectator");
            case "DEAD" -> binary(o, "protocol-bukkit-dead");
            case "DEATH" -> less(o, "death-movement-age", 25);
            case "TELEPORT" -> either(less(o, "protocol-nonpearl-teleport-age", 10), binary(o, "source-server-position-match"));
            case "WEB" -> binary(o, "timer-web-exempt");
            case "VEHICLE" -> binary(o, "timer-vehicle-exempt");
            case "SERVER_POSITION" -> less(o, "teleport-movement-age", 8);
            case "VELOCITY" -> less(o, "source-velocity-position-age", 40);
            case "SOUL_SPEED" -> less(o, "source-soul-speed-position-age", 60);
            case "STAIRS" -> binary(o, "source-stairs-contact");
            case "SLAB" -> binary(o, "source-slab-contact");
            case "FENCE" -> less(o, "source-fence-position-age", 7);
            case "WORLD_CHANGE" -> less(o, "source-world-change-movement-age", 40);
            case "FIREBALL" -> less(o, "source-fireball-damage-movement-age", 25);
            case "BLOCK_PLACE" -> less(o, "source-block-place-movement-age", 30);
            case "BLOCK_BREAK" -> less(o, "source-block-break-movement-age", 25);
            case "PLACED_SLIME" -> less(o, "source-placed-slime-movement-age", 120);
            case "CHORUS_FRUIT" -> less(o, "source-chorus-fruit-movement-age", 25);
            case "CANCELLED_PLACE" -> less(o, "source-cancelled-place-movement-age", 40);
            case "CANCELLED_MOVE" -> less(o, "source-cancelled-move-movement-age", 5);
            case "EMPTIED_BUCKET" -> less(o, "source-emptied-bucket-movement-age", 15);
            case "BUBBLE_COLUMN" -> less(o, "source-bubble-contact-position-age", 40);
            case "HIGH_LEVITATION" -> greater(o, "protocol-levitation-amplifier", 10);
            case "HIGH_JUMP_BOOST" -> greater(o, "protocol-jump-amplifier", 2);
            case "HIGH_SPEED" -> greater(o, "protocol-speed-amplifier", 5);
            case "ENDER_PEARL" -> less(o, "protocol-ender-pearl-age", 5);
            case "FLIGHT" -> {
                Boolean allowed = binary(o, "protocol-bukkit-flight-allowed");
                if (Boolean.TRUE.equals(allowed)) yield Boolean.TRUE;
                if (allowed == null || !o.hasAll("source-flight-position-age", "protocol-flight-cooldown")) yield null;
                yield o.value("source-flight-position-age") < o.value("protocol-flight-cooldown");
            }
            default -> null;
        };
    }

    public static Result evaluate(Observation observation, String... names) {
        boolean known = true;
        for (String name : names) {
            Boolean value = value(observation, name);
            if (Boolean.TRUE.equals(value)) return new Result(true, true);
            if (value == null) known = false;
        }
        return new Result(known, false);
    }

    private static Boolean binary(Observation o, String key) { return !o.has(key) ? null : o.value(key) == 0 ? Boolean.FALSE : o.value(key) == 1 ? Boolean.TRUE : null; }
    private static Boolean less(Observation o, String key, double limit) { return !o.has(key) || !Double.isFinite(o.value(key)) ? null : o.value(key) < limit; }
    private static Boolean greater(Observation o, String key, double limit) { return !o.has(key) || !Double.isFinite(o.value(key)) ? null : o.value(key) > limit; }
    private static Boolean either(Boolean first, Boolean second) { return Boolean.TRUE.equals(first) || Boolean.TRUE.equals(second) ? Boolean.TRUE : first == null || second == null ? null : Boolean.FALSE; }

    public record Result(boolean known, boolean active) {}

}
