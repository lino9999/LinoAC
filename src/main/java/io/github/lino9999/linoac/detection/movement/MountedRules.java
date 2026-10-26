package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;
import java.util.Set;

final class MountedRules {
    static final Set<String> IDS = Set.of("boatfly-a", "boatfly-b", "boatfly-c", "entityflight-a", "entityflight-b", "entityspeed-a", "nosaddle-a");
    private static final Set<String> FLIGHT_TYPES = Set.of("HORSE", "PIG", "DONKEY", "ZOMBIE_HORSE", "SKELETON_HORSE", "CAMEL");
    private static final Set<String> SPEED_TYPES = Set.of("HORSE", "MULE", "DONKEY", "ZOMBIE_HORSE", "SKELETON_HORSE", "PIG", "STRIDER", "LLAMA", "CAMEL");

    void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        if ((!o.state("source-mounted-raw") && !o.state("source-mounted-bukkit"))
                || !Boolean.TRUE.equals(ProtocolExemptions.value(o, "SERVER_VERSION"))) return;
        String type = type(o);
        if (type == null) return;
        if (type.equals("BOAT")) boats(o, sink);
        if (FLIGHT_TYPES.contains(type)) flight(o, sink);
        speed(o, type, memory, sink);
        if (o.state("source-mounted-bukkit") && type.equals("HORSE")) saddle(o, sink);
    }

    private void boats(Observation o, FindingSink sink) {
        if (!finite(o, "packet-count") || value(o, "packet-count") <= 5
                || !known(o, "gravity", "hooked", "other-living-nearby")
                || !active(o, "gravity") || active(o, "hooked") || active(o, "other-living-nearby")) return;
        if (finite(o, "dy", "liquid-age", "slime-age", "bubble-age", "piston-age", "bed-age", "sulfur-age") && known(o, "all-air")) {
            boolean suspicious = value(o, "dy") > .01 && ages(o, 100, "liquid-age", "slime-age", "bubble-age")
                    && value(o, "piston-age") >= 50 && value(o, "bed-age") >= 40 && value(o, "sulfur-age") >= 60 && active(o, "all-air");
            sink.sampleAbove("boatfly-a", suspicious, "mounted-rise=" + value(o, "dy"));
        }
        Boolean waterlogged = ProtocolExemptions.value(o, "WATERLOGGED");
        if (waterlogged != null && finite(o, "horizontal", "dy", "ice-age", "liquid-age", "slime-age", "bubble-age", "piston-age")) {
            double limit = .25 + (waterlogged ? .2 : 0);
            boolean suspicious = value(o, "horizontal") > limit && value(o, "dy") > -.05
                    && ages(o, 100, "ice-age", "liquid-age", "slime-age", "bubble-age", "piston-age");
            sink.sampleAbove("boatfly-b", suspicious, "mounted-horizontal=" + value(o, "horizontal") + " limit=" + limit);
        }
        if (finite(o, "dy", "previous-dy", "air-age", "ice-age", "liquid-age", "slime-age", "bubble-age", "piston-age", "nearby-boat-count")) {
            boolean suspicious = value(o, "dy") > -.05 && Math.abs(value(o, "dy") - value(o, "previous-dy")) < .025
                    && value(o, "air-age") > 5 && ages(o, 50, "ice-age", "liquid-age", "slime-age", "bubble-age", "piston-age")
                    && value(o, "nearby-boat-count") == 1;
            sink.sampleAbove("boatfly-c", suspicious, "mounted-hover=" + value(o, "dy"));
        }
    }

    private void flight(Observation o, FindingSink sink) {
        if (!finite(o, "y", "packet-count") || value(o, "y") < 0 || value(o, "packet-count") <= 5
                || !known(o, "leashed", "levitation", "slow-falling")
                || active(o, "leashed") || active(o, "levitation") || active(o, "slow-falling")) return;
        if (finite(o, "air-age", "dy", "liquid-age", "slime-age", "bubble-age", "sulfur-age")) {
            boolean suspicious = value(o, "air-age") > 30 && value(o, "dy") > 0
                    && ages(o, 100, "liquid-age", "slime-age", "bubble-age") && value(o, "sulfur-age") >= 60;
            sink.sampleAbove("entityflight-a", suspicious, "mounted-air-rise=" + value(o, "dy"));
        }
        if (finite(o, "air-age", "dy", "previous-dy", "liquid-age", "slime-age", "bubble-age")) {
            double dy = value(o, "dy");
            boolean suspicious = value(o, "air-age") > 30 && Math.abs(dy) < 5
                    && Math.abs(dy - value(o, "previous-dy")) < .05 && dy > -1.15
                    && ages(o, 100, "liquid-age", "slime-age", "bubble-age");
            sink.sampleAbove("entityflight-b", suspicious, "mounted-air-residual=" + Math.abs(dy - value(o, "previous-dy")));
        }
    }

    private void speed(Observation o, String type, DetectionMemory memory, FindingSink sink) {
        if (!known(o, "living")) return;
        double effectAge = memory.get("movement.mounted-speed-effect-age", 10000);
        if (active(o, "living")) {
            if (!finite(o, "speed-level")) return;
            effectAge = value(o, "speed-level") > 0 ? 0 : Math.min(10000, effectAge + 1);
            memory.set("movement.mounted-speed-effect-age", effectAge);
        }
        if (!o.has("source-entityspeed-a-min-difference") || !Double.isFinite(o.value("source-entityspeed-a-min-difference"))
                || !finite(o, "horizontal", "packet-count", "ice-age")) return;
        double horizontal = value(o, "horizontal"), limit;
        if (type.equals("BOAT")) {
            if (!finite(o, "liquid-age", "bubble-age", "slime-age")) return;
            limit = .65 + (value(o, "liquid-age") < 100 ? .15 : 0);
            sink.sampleAbove("entityspeed-a", horizontal - limit > o.value("source-entityspeed-a-min-difference")
                    && ages(o, 100, "ice-age", "bubble-age", "slime-age") && value(o, "packet-count") > 5,
                    "mounted-speed=" + horizontal + " limit=" + limit);
            return;
        }
        if (!SPEED_TYPES.contains(type) || !finite(o, "base-speed", "speed-level", "y") || !known(o, "leashed") || active(o, "leashed")) return;
        if (type.equals("HORSE") && (!known(o, "cosmetic") || active(o, "cosmetic"))) return;
        boolean pig = type.equals("PIG"), skeleton = type.equals("SKELETON_HORSE"), camel = type.equals("CAMEL");
        double entityCoefficient = switch (type) {
            case "SKELETON_HORSE", "STRIDER" -> .15;
            case "LLAMA", "CAMEL" -> .45;
            default -> .5;
        };
        double playerCoefficient = skeleton || type.equals("STRIDER") ? .065 : .085;
        limit = value(o, "base-speed") * 2.25 + (camel ? .45 : .1) + value(o, "speed-level") * entityCoefficient;
        boolean aligned = value(o, "y") % .015625 == 0;
        if (!pig) {
            if (!o.has("protocol-speed-amplifier") || !Double.isFinite(o.value("protocol-speed-amplifier"))) return;
            limit += o.value("protocol-speed-amplifier") * playerCoefficient;
            if (!aligned) limit += .17499999701976776;
        }
        if (skeleton) {
            if (!finite(o, "liquid-age")) return;
            if (value(o, "liquid-age") < 20) limit += .3;
        }
        if (camel) {
            if (!known(o, "dashing")) return;
            if (active(o, "dashing")) limit += .25;
        }
        if (effectAge > 0 && effectAge < 20) limit += .5;
        boolean suspicious = horizontal - limit > o.value("source-entityspeed-a-min-difference") && value(o, "ice-age") >= 100
                && value(o, "packet-count") > 10 && (!pig || aligned);
        sink.sampleAbove("entityspeed-a", suspicious, "mounted-speed=" + horizontal + " limit=" + limit);
    }

    private void saddle(Observation o, FindingSink sink) {
        if (!finite(o, "base-speed", "horizontal", "packet-count") || !known(o, "saddled")) return;
        double limit = value(o, "base-speed") * 2.5 + .1, horizontal = value(o, "horizontal");
        sink.sampleAbove("nosaddle-a", limit - horizontal < .3 && horizontal > .15 && !active(o, "saddled") && value(o, "packet-count") > 5,
                "unsaddled-speed=" + horizontal + " limit=" + limit);
    }

    private String type(Observation o) {
        if (known(o, "boat") && active(o, "boat")) return "BOAT";
        if (known(o, "camel") && active(o, "camel")) return "CAMEL";
        if (known(o, "llama") && active(o, "llama")) return "LLAMA";
        for (String state : o.states()) if (state.startsWith("source-mounted-type:")) {
            String type = state.substring("source-mounted-type:".length());
            if (type.equals("TRADER_LLAMA")) return "LLAMA";
            return type.endsWith("_BOAT") || type.endsWith("_RAFT") || type.equals("BOAT") || type.equals("CHEST_BOAT") ? "BOAT" : type;
        }
        return null;
    }
    private static double value(Observation o, String key) { return o.value("source-mounted-" + key); }
    private static boolean finite(Observation o, String... keys) {
        for (String key : keys) if (!o.has("source-mounted-" + key) || !Double.isFinite(value(o, key))) return false;
        return true;
    }
    private static boolean known(Observation o, String... keys) {
        for (String key : keys) if (!finite(o, key) || value(o, key) != 0 && value(o, key) != 1) return false;
        return true;
    }
    private static boolean active(Observation o, String key) { return value(o, key) == 1; }
    private static boolean ages(Observation o, int minimum, String... keys) {
        for (String key : keys) if (value(o, key) < minimum) return false;
        return true;
    }
}
