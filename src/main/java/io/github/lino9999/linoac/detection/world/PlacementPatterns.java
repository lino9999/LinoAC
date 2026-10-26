package io.github.lino9999.linoac.detection.world;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

import java.util.Set;

final class PlacementPatterns {
    static final Set<String> IDS = Set.of("airplace-a", "baritone-a", "baritone-b", "tower-a", "scaffold-a", "scaffold-b", "scaffold-c",
            "scaffold-d", "scaffold-e", "scaffold-f", "scaffold-g", "scaffold-h", "scaffold-i", "scaffold-k", "scaffold-m");

    void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        var timeline = new WorldSequenceTimeline(memory);
        timeline.observe(o);
        if (o.kind() == Observation.Kind.MOVE) flying(o, timeline, sink);
        if (o.kind() == Observation.Kind.ROTATION) rotations(o, timeline, sink);
        if (o.kind() == Observation.Kind.USE && o.state("bukkit-event") && o.state("world-right-click-block")) interaction(o, timeline, sink);
        if (o.kind() != Observation.Kind.PLACE || !o.state("bukkit-event") || !time(o, "event-time-ms")) return;
        double now = o.value("event-time-ms");
        Boolean bridge = PlacementContext.bridge(o);
        timeline.placement(bridge, o.state("block-accepted"), now);
        if (!o.state("block-accepted")) return;
        airPlace(o, sink);
        sprintPlacement(o, timeline, now, sink);
        if (!o.state("world-material-known")) return;
        facePlacement(o, timeline, bridge, now, sink);
        sneakPlacement(o, timeline, bridge, now, sink);
        directionPlacement(o, timeline, bridge, now, sink);
        bridgeBatch(o, timeline, bridge, now, sink);
        expansion(o, sink);
        tower(o, timeline, now, sink);
        if (Boolean.TRUE.equals(bridge) && finite(o, "source-horizontal", "source-previous-horizontal", "original-delta-yaw", "original-delta-pitch")
                && Math.abs(o.value("source-horizontal") - o.value("source-previous-horizontal")) < .01
                && (float) o.value("original-delta-yaw") > 150f && (float) o.value("original-delta-pitch") > 10f) {
            sink.direct("scaffold-i", "Bridge direction changed while horizontal motion stayed stable.");
        }
    }

    private void airPlace(Observation o, FindingSink sink) {
        if (!o.state("world-material-known") || o.state("world-airplace-material-exempt")
                || !finite(o, "world-air-neighbors", "block-x", "block-y", "block-z", "against-x", "against-y", "against-z")) return;
        var exempt = ProtocolExemptions.evaluate(o, "SPECTATOR", "CANCELLED_PLACE", "BLOCK_BREAK");
        if (!exempt.known() || exempt.active()) return;
        if (o.value("world-air-neighbors") == 6 && o.value("block-x") == o.value("against-x") && o.value("block-y") == o.value("against-y")
                && o.value("block-z") == o.value("against-z")) sink.direct("airplace-a", "Placement had no neighboring support.");
    }

    private void interaction(Observation o, WorldSequenceTimeline timeline, FindingSink sink) {
        if (!time(o, "event-time-ms") || !time(o, "world-player-created-ms") || !finite(o, "block-face")) return;
        double now = o.value("event-time-ms");
        if (now - o.value("world-player-created-ms") < 10000 || timeline.elapsed(WorldSequenceTimeline.Clock.INTERACT, now) < 10000) return;
        var exempt = ProtocolExemptions.evaluate(o, "TELEPORT", "SERVER_POSITION");
        if (exempt.known() && !exempt.active() && o.state("clicked-below-feet") && o.state("clicked-solid")
                && o.state("held-solid") && o.value("block-face") == 0) sink.direct("scaffold-a", "Solid block interaction used its lower face beneath the player.");
    }

    private void flying(Observation o, WorldSequenceTimeline timeline, FindingSink sink) {
        if (timeline.window(WorldSequenceTimeline.Window.ROTATION_SNAP, 3) && finite(o, "original-delta-yaw", "original-delta-pitch")) {
            sink.sampleAbove("scaffold-e", (float) o.value("original-delta-yaw") > 100f && (float) o.value("original-delta-pitch") > 8f,
                    "Bridge placement was followed by a large rotation.");
        }
    }

    private void rotations(Observation o, WorldSequenceTimeline timeline, FindingSink sink) {
        if (!finite(o, "original-delta-yaw", "original-delta-pitch")) return;
        float yaw = (float) o.value("original-delta-yaw"), pitch = (float) o.value("original-delta-pitch");
        if (timeline.smallRotationAge() < 10) {
            Boolean numeric = yaw > 0 && (double) yaw < .005 && pitch > 0 && (double) pitch < .005;
            sample(o, sink, "baritone-a", numeric, binary(o, "cinematic-estimate"), "Small rotations followed cancelled digging.");
        }
        if (timeline.largeRotationAge() < 5 && finite(o, "source-pitch")) {
            sample(o, sink, "baritone-b", yaw > 95f && (double) pitch < .15 && Math.abs((float) o.value("source-pitch")) < 70f,
                    exempt(o, "PISTON"), "Yaw changed sharply with nearly fixed pitch.");
        }
        if (timeline.window(WorldSequenceTimeline.Window.PITCH_PATTERN, 6) && finite(o, "original-previous-delta-pitch")) {
            long pattern = WorldPatternMath.pitchPattern(pitch, (float) o.value("original-previous-delta-pitch"));
            sample(o, sink, "scaffold-d", pattern > 0 && pattern < 131072 && (double) pitch > .5,
                    either(exempt(o, "CINEMATIC"), binary(o, "source-swift-sneak")), "Bridge rotation used a small fixed-point pitch pattern.");
        }
        if (timeline.window(WorldSequenceTimeline.Window.SENSITIVITY, 5) && finite(o, "instant-sensitivity", "source-horizontal")) {
            double sensitivity = o.value("instant-sensitivity");
            sample(o, sink, "scaffold-h", (sensitivity < -2 || sensitivity > 200) && pitch > .55f && o.value("source-horizontal") > .25,
                    either(exempt(o, "CINEMATIC"), binary(o, "source-swift-sneak")), "Bridge pitch sensitivity fell outside the client range.");
        }
    }

    private void sprintPlacement(Observation o, WorldSequenceTimeline timeline, double now, FindingSink sink) {
        double interval = timeline.elapsed(WorldSequenceTimeline.Clock.SPRINT_PLACE, now);
        timeline.commit(WorldSequenceTimeline.Clock.SPRINT_PLACE, now);
        if (timeline.bridgeAge() < 0 || timeline.sprintAge() < 0) return;
        sample(o, sink, "scaffold-c", timeline.bridgeAge() < 1 && timeline.sprintAge() > 5 && interval >= 0 && interval < 300,
                exempt(o, "LIQUID", "CREATIVE", "FLIGHT"), "Sprinting continued through rapid bridge placements.");
    }

    private void facePlacement(Observation o, WorldSequenceTimeline timeline, Boolean bridge, double now, FindingSink sink) {
        Boolean cancelled = exempt(o, "CANCELLED_PLACE");
        if (!Boolean.FALSE.equals(cancelled)) return;
        double interval = timeline.elapsed(WorldSequenceTimeline.Clock.BRIDGE_FACE, now);
        timeline.commit(WorldSequenceTimeline.Clock.BRIDGE_FACE, now);
        if (!finite(o, "block-face")) return;
        Boolean numeric = and(bridge, o.value("block-face") == 6, interval >= 0 && interval < 300);
        sample(o, sink, "scaffold-b", numeric, either(o.state("world-face-material-exempt"), exempt(o, "LIQUID", "CREATIVE", "FLIGHT", "TURTLE_EGG", "SLAB")),
                "Bridge placement referenced its own attachment face.");
    }

    private void sneakPlacement(Observation o, WorldSequenceTimeline timeline, Boolean bridge, double now, FindingSink sink) {
        if (o.state("world-scaffolding-material") || !o.state("placed-solid")) return;
        double interval = timeline.elapsed(WorldSequenceTimeline.Clock.SNEAK_PLACE, now);
        timeline.commit(WorldSequenceTimeline.Clock.SNEAK_PLACE, now);
        Boolean numeric = and(timeline.sneakPending(), bridge, interval >= 0 && interval < 400);
        if (Boolean.FALSE.equals(numeric)) sample(o, sink, "scaffold-f", false, false, "Sneak placement sequence was clean.");
        else if (finite(o, "source-horizontal", "source-speed-amplifier", "source-walk-speed")) {
            float limit = WorldPatternMath.sneakingThreshold((float) o.value("source-speed-amplifier"), (float) o.value("source-walk-speed"));
            sample(o, sink, "scaffold-f", and(numeric, o.value("source-horizontal") > (double) limit), exempt(o, "LIQUID", "FLIGHT", "CREATIVE", "VEHICLE"),
                    "Sneak transition preceded fast bridge motion.");
        }
        timeline.consumeSneak();
    }

    private void directionPlacement(Observation o, WorldSequenceTimeline timeline, Boolean bridge, double now, FindingSink sink) {
        if (o.state("world-slab-material") || !Boolean.FALSE.equals(either(binary(o, "source-swift-sneak"), exempt(o, "SLAB")))
                || !Boolean.TRUE.equals(bridge)) return;
        double interval = timeline.elapsed(WorldSequenceTimeline.Clock.DIRECTION_PLACE, now);
        timeline.commit(WorldSequenceTimeline.Clock.DIRECTION_PLACE, now);
        if (!finite(o, "source-horizontal", "source-previous-horizontal", "source-position-dx", "source-position-dz", "source-yaw")) return;
        double acceleration = Math.abs(o.value("source-horizontal") - o.value("source-previous-horizontal"));
        float angle = WorldPatternMath.directionAngle(o.value("source-position-dx"), o.value("source-position-dz"), (float) o.value("source-yaw"));
        sink.sampleAbove("scaffold-g", acceleration > 0 && acceleration < .001 && interval >= 0 && interval < 300 && angle < 2f
                && !o.state("world-scaffolding-material"), "Bridge direction matched stable horizontal acceleration.");
    }

    private void bridgeBatch(Observation o, WorldSequenceTimeline timeline, Boolean bridge, double now, FindingSink sink) {
        if (o.state("world-slab-material") || !Boolean.TRUE.equals(bridge) || timeline.sneakGrace(o.tick())
                || !Boolean.FALSE.equals(either(binary(o, "source-swift-sneak"), exempt(o, "SLAB")))) return;
        Integer count = timeline.batch(now);
        if (count != null) sink.sampleAbove("scaffold-k", count >= o.value("bridge-placement-limit", 6), "Bridge placement batch contained " + count + " blocks.");
    }

    private void expansion(Observation o, FindingSink sink) {
        if (o.state("world-scaffolding-material") || !finite(o, "block-x", "block-y", "block-z", "source-position-x", "source-position-y", "source-position-z", "source-yaw", "block-face")) return;
        double dx = o.value("source-position-x") - o.value("block-x"), dz = o.value("source-position-z") - o.value("block-z");
        double distance = Math.sqrt(dx * dx + dz * dz);
        sink.sampleAbove("scaffold-m", o.value("block-y") < o.value("source-position-y") && distance > 1 && distance < 6
                && o.value("block-face") == WorldPatternMath.facing((float) o.value("source-yaw")), "Placement expanded along the current view direction.");
    }

    private void tower(Observation o, WorldSequenceTimeline timeline, double now, FindingSink sink) {
        if (!o.state("placed-solid")) return;
        double interval = timeline.elapsed(WorldSequenceTimeline.Clock.TOWER_PLACE, now);
        timeline.commit(WorldSequenceTimeline.Clock.TOWER_PLACE, now);
        if (!finite(o, "event-player-floor-x", "event-player-floor-z", "block-x", "block-z", "block-face", "source-horizontal", "source-delta-y")) return;
        boolean numeric = o.value("block-face") == 1 && o.value("event-player-floor-x") == o.value("block-x") && o.value("event-player-floor-z") == o.value("block-z")
                && interval >= 0 && interval < 250 && o.value("source-delta-y") > 0 && o.value("source-horizontal") < .1;
        sample(o, sink, "tower-a", numeric, either(o.state("world-tower-material-exempt"), exempt(o, "CREATIVE", "FLIGHT", "JUMP_BOOST", "CANCELLED_PLACE", "LIQUID",
                "TURTLE_EGG", "CLIMBABLE", "EXPLOSION", "ELYTRA", "PACKET_EXPLOSION", "SLOW_FALLING")), "Rapid upward placements followed rising motion in the player column.");
    }

    private static void sample(Observation o, FindingSink sink, String id, Boolean numeric, Boolean exempt, String detail) {
        if (Boolean.FALSE.equals(numeric) || Boolean.TRUE.equals(exempt)) sink.sampleAbove(id, false, detail);
        else if (Boolean.TRUE.equals(numeric) && Boolean.FALSE.equals(exempt)) sink.sampleAbove(id, true, detail);
    }

    private static Boolean exempt(Observation o, String... names) {
        var result = ProtocolExemptions.evaluate(o, names);
        return result.active() ? Boolean.TRUE : result.known() ? Boolean.FALSE : null;
    }

    private static Boolean binary(Observation o, String key) { return !o.has(key) ? null : o.value(key) == 0 ? Boolean.FALSE : o.value(key) == 1 ? Boolean.TRUE : null; }
    private static Boolean either(Boolean first, Boolean second) { return Boolean.TRUE.equals(first) || Boolean.TRUE.equals(second) ? Boolean.TRUE : first == null || second == null ? null : Boolean.FALSE; }
    private static Boolean and(Boolean... operands) {
        boolean unknown = false;
        for (Boolean operand : operands) { if (Boolean.FALSE.equals(operand)) return false; if (operand == null) unknown = true; }
        return unknown ? null : Boolean.TRUE;
    }
    private static boolean finite(Observation o, String... keys) { for (String key : keys) if (!o.has(key) || !Double.isFinite(o.value(key))) return false; return true; }
    private static boolean time(Observation o, String key) { return finite(o, key) && o.value(key) >= 0 && o.value(key) == Math.rint(o.value(key)); }
}
