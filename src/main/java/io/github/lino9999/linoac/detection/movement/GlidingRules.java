package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;

import java.util.Set;

final class GlidingRules {
    static final Set<String> IDS = Set.of("elytra-a", "elytra-b", "elytra-c", "elytra-f", "elytra-g", "elytra-i", "elytra-k", "elytra-l", "elytra-m");
    private static final String[] SPEED = {"RIPTIDE", "TELEPORT", "WORLD_CHANGE", "DEATH", "ENDER_PEARL", "CHORUS_FRUIT", "SERVER_POSITION_FAST", "DRAGON_DAMAGE", "DOLPHINS_GRACE"};
    private static final String[] STABLE = {"FIREWORK", "LIQUID"};
    private static final String[] ACCELERATION = {"FIREWORK", "EXPLOSION", "RIPTIDE", "CHUNK", "DRAGON_DAMAGE", "FLIGHT", "SLIME"};
    private static final String[] FAST_ACCELERATION = {"FIREWORK", "EXPLOSION", "RIPTIDE", "CHUNK", "DRAGON_DAMAGE", "FLIGHT", "VELOCITY", "SLIME", "DOLPHINS_GRACE"};
    private static final String[] HORIZONTAL_GRID = {"FIREWORK", "EXPLOSION", "RIPTIDE", "WEB", "DRAGON_DAMAGE", "FLIGHT", "VELOCITY", "LIQUID"};
    private static final String[] ASCENT = {"FIREWORK", "EXPLOSION", "RIPTIDE", "DRAGON_DAMAGE", "FLIGHT", "VELOCITY", "SLIME", "DOLPHINS_GRACE", "LIQUID"};
    private static final String[] VERTICAL_SPEED = {"DRAGON_DAMAGE", "EXPLOSION", "FIREWORK", "TELEPORT", "FISHING_ROD", "RIPTIDE", "BUBBLE_COLUMN", "LIQUID", "SLIME", "PISTON", "WORLD_CHANGE", "SWIMMING", "BED", "LEVITATION", "FLIGHT"};
    private static final String[] VERTICAL_GRID = {"FLIGHT", "SLIME", "SLOW_FALLING", "LEVITATION", "LIQUID", "BUBBLE_COLUMN", "LIQUID", "RIPTIDE", "CLIMBABLE"};

    void evaluate(Observation input, DetectionMemory memory, FindingSink sink) {
        action(input, memory, sink);
        if (input.kind() != Observation.Kind.MOVE || !input.state("position-packet")
                || !Boolean.TRUE.equals(MovementRuleInputs.bit(input, "source-physical-gliding"))) return;
        boolean modern = Boolean.TRUE.equals(MovementRuleInputs.bit(input, "protocol-server-at-least-1-9"));
        if (modern) {
            stable(input, sink);
            acceleration(input, sink);
            if (MovementRuleInputs.outer(input, true, false)) horizontal(input, sink);
        }
        if (MovementRuleInputs.outer(input, true, false)) vertical(input, sink);
    }

    private void action(Observation input, DetectionMemory memory, FindingSink sink) {
        if (input.state("bukkit-event") || !input.state("action:START_FLYING_WITH_ELYTRA") || !input.state("packet:ENTITY_ACTION")
                || !MovementRuleInputs.outer(input, true, false) || !Boolean.TRUE.equals(MovementRuleInputs.bit(input, "protocol-server-at-least-1-9"))
                || !MovementRuleInputs.finite(input, "packet-time-ms")) return;
        double now = input.value("packet-time-ms"), elapsed = now - memory.get("movement.gliding.last-start-ms");
        MovementRuleInputs.strict(input, sink, "elytra-f", elapsed < 110, "glide-start-interval-ms=" + elapsed, "FAST");
        memory.set("movement.gliding.last-start-ms", now);
    }

    private void stable(Observation input, FindingSink sink) {
        if (!MovementRuleInputs.finite(input, "source-physical-gliding-position-count", "source-horizontal", "source-previous-horizontal", "source-delta-y", "source-previous-position-dy")) return;
        double age = input.value("source-physical-gliding-position-count");
        MovementRuleInputs.strict(input, sink, "elytra-b", age > 5 && age < 400 && Math.abs(input.value("source-horizontal") - input.value("source-previous-horizontal")) < 1e-10
                && Math.abs(input.value("source-delta-y") - input.value("source-previous-position-dy")) < 1e-10 && input.value("source-horizontal") > 1,
                "stable-glide-speed=" + input.value("source-horizontal"), STABLE);
    }

    private void acceleration(Observation input, FindingSink sink) {
        if (!MovementRuleInputs.finite(input, "source-horizontal", "source-previous-horizontal", "source-delta-y")) return;
        double horizontal = input.value("source-horizontal"), acceleration = horizontal - input.value("source-previous-horizontal"), dy = input.value("source-delta-y");
        if (MovementRuleInputs.finite(input, "source-physical-gliding-position-count", "source-cached-air-position-age")
                && MovementRuleInputs.bit(input, "source-bukkit-gliding") != null) {
            MovementRuleInputs.strict(input, sink, "elytra-c", input.value("source-physical-gliding-position-count") > 3
                    && input.value("source-cached-air-position-age") > 3 && input.value("source-bukkit-gliding") == 1 && acceleration > .035 && dy > -.6,
                    "glide-horizontal-acceleration=" + acceleration, ACCELERATION);
        }
        MovementRuleInputs.strict(input, sink, "elytra-g", acceleration > .031 && dy > -.5 && horizontal > .65,
                "fast-glide-horizontal-acceleration=" + acceleration, FAST_ACCELERATION);
    }

    private void horizontal(Observation input, FindingSink sink) {
        if (!MovementRuleInputs.finite(input, "source-horizontal")) return;
        double horizontal = input.value("source-horizontal");
        if (MovementRuleInputs.finite(input, "source-elytra-a-max-speed", "entity-cram-position-age", "source-riptide-position-age")) {
            MovementRuleInputs.strict(input, sink, "elytra-a", horizontal > input.value("source-elytra-a-max-speed")
                    && input.value("entity-cram-position-age") >= 200 && input.value("source-riptide-position-age") >= 400,
                    "glide-horizontal-speed=" + horizontal, SPEED);
        }
        double grid = .1 % (horizontal % .1);
        MovementRuleInputs.strict(input, sink, "elytra-i", grid < 1e-8 && horizontal > .1, "glide-horizontal-grid=" + grid, HORIZONTAL_GRID);
        if (MovementRuleInputs.finite(input, "source-previous-horizontal", "source-delta-y", "source-physical-gliding-position-count")) {
            MovementRuleInputs.strict(input, sink, "elytra-k", horizontal > .3 && input.value("source-delta-y") > 0
                    && horizontal - input.value("source-previous-horizontal") > .005 && input.value("source-physical-gliding-position-count") > 5,
                    "rising-glide-horizontal-speed=" + horizontal, ASCENT);
        }
    }

    private void vertical(Observation input, FindingSink sink) {
        if (!MovementRuleInputs.finite(input, "source-delta-y")) return;
        double dy = input.value("source-delta-y");
        if (Math.abs(dy) > .01) {
            double grid = .01 % (dy % .01);
            MovementRuleInputs.strict(input, sink, "elytra-m", grid < 1e-8, "glide-vertical-grid=" + grid, VERTICAL_GRID);
        }
        if (!MovementRuleInputs.finite(input, "source-pitch", "entity-cram-position-age", "source-riptide-position-age", "source-velocity-position-age", "source-maximum-vertical-velocity",
                "source-explosion-position-age", "source-last-positive-explosion-y", "protocol-jump-amplifier", "source-unassigned-effect-level")) return;
        double limit = 1.45;
        if (input.value("source-velocity-position-age") < 30) limit += input.value("source-maximum-vertical-velocity");
        if (input.value("source-explosion-position-age") < 30) limit += input.value("source-last-positive-explosion-y");
        if (input.value("protocol-jump-amplifier") > 0) limit += input.value("protocol-jump-amplifier") * .125;
        if (input.value("source-unassigned-effect-level") > 0) limit += input.value("protocol-jump-amplifier") * .125;
        MovementRuleInputs.strict(input, sink, "elytra-l", dy > limit && (float) input.value("source-pitch") > -20f
                && input.value("entity-cram-position-age") >= 200 && input.value("source-riptide-position-age") >= 400,
                "glide-vertical-speed=" + dy + " limit=" + limit, VERTICAL_SPEED);
    }
}
