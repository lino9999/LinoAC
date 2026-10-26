package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import java.util.Set;

public final class InvalidRules implements RuleModule {
    private static final Set<String> IDS = Set.of("invalid-a", "invalid-b", "invalid-c", "invalid-e", "invalid-f", "invalid-i", "invalid-j");
    private static final String[] C = {"TELEPORT", "RIPTIDE", "GLIDING", "FISHING_ROD", "VELOCITY", "JOINED", "FLIGHT", "CREATIVE", "VEHICLE", "WORLD_CHANGE", "DOLPHINS_GRACE", "SOUL_SPEED", "DEATH", "SLEEPING", "ENDER_PEARL", "HIGH_JUMP_BOOST", "HIGH_LEVITATION"};
    private static final String[] E = {"TELEPORT", "RIPTIDE", "GLIDING", "VELOCITY", "JOINED", "FLIGHT", "CREATIVE", "VEHICLE", "WORLD_CHANGE", "DOLPHINS_GRACE", "SOUL_SPEED", "CHORUS_FRUIT", "SLEEPING", "LEVITATION", "ENDER_PEARL", "ENTITY_CRAM_FIX", "HIGH_SPEED", "SHULKER_BOX", "SHULKER", "CHUNK"};
    private static final String[] I = {"JOINED", "TELEPORT", "RIPTIDE", "HONEY", "GLIDING", "LEVITATION", "SLAB", "STAIRS", "FLIGHT", "JUMP_BOOST_RAN_OUT", "ELYTRA", "BUBBLE_COLUMN", "WALL", "CREATIVE", "SHULKER_BOX", "ANVIL", "SLIME", "PISTON", "TURTLE_EGG", "DOOR", "DEATH", "ENDER_PEARL", "PLACED_SLIME", "CHORUS_FRUIT", "MYTHIC_MOB", "BLOCK_BREAK", "FISHING_ROD", "SLEEPING", "SHULKER"};
    private static final String[] J = {"FLIGHT", "JOINED", "CREATIVE", "SHULKER", "SHULKER_BOX", "BAMBOO", "CAKE", "PRESSURE_PLATE", "CHUNK", "DOLPHINS_GRACE", "TELEPORT", "ILLEGAL_BLOCK", "GLIDING", "DOOR", "RIPTIDE", "VEHICLE", "BOAT", "FISHING_ROD", "ELYTRA", "SIGN", "DEATH", "SLEEPING", "ENDER_PEARL", "FROZEN", "CHORUS_FRUIT", "SPECTATOR", "WORLD_CHANGE", "ANVIL", "CANCELLED_MOVE", "GLASS_PANE", "FENCE", "BED"};

    @Override
    public Set<String> checkIds() { return IDS; }

    @Override
    public void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        if (o.kind() != Observation.Kind.MOVE || !o.state("position-packet") || o.state("bukkit-event")) return;
        if (o.hasAll("protocol-flight-allowed", "protocol-bukkit-flight-allowed")) {
            boolean mismatch = o.value("protocol-flight-allowed") == 1 && o.value("protocol-bukkit-flight-allowed") == 0;
            double count = mismatch ? memory.increment("invalid.flight-mismatch") : 0;
            memory.set("invalid.flight-mismatch", count);
            if (count > 100) sink.direct("invalid-a", "Acknowledged flight permissions disagreed with the server for over 100 position packets");
        }
        if (o.has("protocol-ghost-corrections") && o.value("protocol-ghost-corrections") > 5) sink.direct("invalid-b", "Repeated authoritative ghost block corrections in the movement window");
        if (o.state("server-teleport-match")) return;
        if (o.has("source-position-dy")) {
            double dy = o.value("source-position-dy");
            var exemptions = ProtocolExemptions.evaluate(o, "DEATH", "JOINED", "TELEPORT", "WORLD_CHANGE");
            if (exemptions.known()) sink.sampleAbove("invalid-f", Math.abs(dy) > 100 && !exemptions.active(), "Vertical displacement exceeded 100 blocks");
        }
        legacyJump(o, sink);
        horizontal(o, sink);
        if (!o.has("timer-origin-skip") || o.value("timer-origin-skip") == 1) return;
        if (o.hasAll("source-position-horizontal", "protocol-movement-speed")) {
            double horizontal = o.value("source-position-horizontal");
            var exemptions = ProtocolExemptions.evaluate(o, E);
            if (exemptions.known()) sink.sampleAbove("invalid-e", horizontal > 3.5 && horizontal < 100 && o.value("protocol-movement-speed") < .25 && !exemptions.active(), "Horizontal displacement exceeded the supported motion range");
        }
        vertical(o, sink);
    }

    private static void vertical(Observation o, FindingSink sink) {
        if (!o.hasAll("source-position-dy", "protocol-levitation-amplifier", "protocol-nonpearl-teleport-age", "death-movement-age", "join-age-ms", "source-explosion-pending", "source-explosion-sum-y", "source-last-positive-explosion-y", "source-explosion-position-age")) return;
        if (o.value("protocol-levitation-amplifier") > 100) return;
        var exemptions = ProtocolExemptions.evaluate(o, C);
        if (!exemptions.known() || !ProtocolExemptions.known(o, "SERVER_POSITION")) return;
        double dy = o.value("source-position-dy");
        double maximum = InvalidMotionBudget.vertical(o.value("source-explosion-pending") == 1, o.value("source-explosion-sum-y"), o.value("source-last-positive-explosion-y"), (int) o.value("source-explosion-position-age"));
        boolean suspicious = Math.abs(dy) > maximum && o.value("protocol-nonpearl-teleport-age") >= 80 && o.value("death-movement-age") >= 40 && o.value("join-age-ms") >= 10000 && !exemptions.active();
        if (suspicious) sink.sampleAboveOrDirect("invalid-c", dy > 4 && ProtocolExemptions.any(o, "SERVER_POSITION"), "Vertical displacement exceeded the acknowledged explosion budget");
        else sink.sampleAbove("invalid-c", false, "Vertical displacement remained within the acknowledged motion budget");
    }

    private static void legacyJump(Observation o, FindingSink sink) {
        if (!o.has("client-at-least-1-9") || o.value("client-at-least-1-9") != 0) return;
        if (!o.hasAll("source-position-dy", "protocol-air-run", "source-nonfall-damage-movement-age", "source-bed-position-age", "source-velocity-position-age", "source-dragon-damage-movement-age", "timer-vehicle-exempt")) return;
        var maximum = InvalidMotionBudget.legacyVertical(o);
        if (maximum.isEmpty()) return;
        var exemptions = ProtocolExemptions.evaluate(o, I);
        if (!exemptions.known()) return;
        double dy = o.value("source-position-dy");
        boolean suspicious = o.value("protocol-air-run") > 0 && dy > maximum.getAsDouble() && o.value("source-nonfall-damage-movement-age") >= 20
                && o.value("source-bed-position-age") >= 40 && o.value("source-velocity-position-age") >= 100 && o.value("source-dragon-damage-movement-age") >= 200
                && o.value("timer-vehicle-exempt") == 1 && !exemptions.active();
        if (suspicious) sink.sampleAboveOrDirect("invalid-i", dy > 2.5, "Legacy upward motion exceeded the acknowledged jump budget");
        else sink.sampleAbove("invalid-i", false, "Legacy upward motion remained within its motion budget");
    }

    private static void horizontal(Observation o, FindingSink sink) {
        if (!o.hasAll("protocol-solid-center", "protocol-jump-amplifier", "protocol-invalid-j-skip") || o.value("protocol-solid-center") != 1 || o.value("protocol-jump-amplifier") > 50 || o.value("protocol-invalid-j-skip") == 1) return;
        if (!o.hasAll("protocol-invalid-j-threshold", "protocol-invalid-j-exempt", "source-position-horizontal")) return;
        var maximum = InvalidMotionBudget.horizontal(o);
        if (maximum.isEmpty()) return;
        var exemptions = ProtocolExemptions.evaluate(o, J);
        if (!exemptions.known() || !ProtocolExemptions.known(o, "SERVER_POSITION", "CHORUS_FRUIT")) return;
        double excess = o.value("source-position-horizontal") - maximum.getAsDouble();
        boolean suspicious = excess > o.value("protocol-invalid-j-threshold") && o.value("protocol-ground-run") >= 0 && o.value("protocol-invalid-j-exempt") == 0 && !exemptions.active();
        if (suspicious) sink.sampleAboveOrDirect("invalid-j", excess > .5 && excess < 100 && !ProtocolExemptions.any(o, "SERVER_POSITION", "CHORUS_FRUIT"), "Horizontal displacement exceeded the retained collision motion budget");
        else sink.sampleAbove("invalid-j", false, "Horizontal displacement remained within the retained motion budget");
    }

}
