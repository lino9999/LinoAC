package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

import java.util.Set;

final class HeadingRules {
    static final Set<String> IDS = Set.of("sprint-a", "strafe-a", "vclip-a");
    private static final String[] SPRINT = {"RIPTIDE", "TELEPORT", "WORLD_CHANGE", "JOINED", "SERVER_POSITION", "SWIMMING", "LIQUID", "ICE", "SWIMMING", "SERVER_POSITION"};
    private static final String[] STRAFE = {"TELEPORT", "PARTIALLY_STUCK", "FIREWORK", "DEATH", "JOINED", "WORLD_CHANGE", "FLIGHT", "FULLY_STUCK", "GLIDING", "CREATIVE", "SPECTATOR", "SOUL_SAND", "ELYTRA", "VEHICLE", "ENTITY_COLLISION", "DEPTH_STRIDER", "FROZEN", "LIQUID", "DOLPHINS_GRACE", "RIPTIDE", "SWIMMING", "CLIMBABLE", "ENDER_PEARL", "FIREBALL", "CHORUS_FRUIT", "BLOCK_BREAK", "CANCELLED_MOVE"};
    private static final String[] CLIP = {"VELOCITY", "JOINED", "TELEPORT", "LIQUID", "CLIMBABLE", "GLIDING", "RIPTIDE", "COLLIDING_VERTICALLY", "HOPPER", "SLIME", "SCAFFOLDING", "BOAT", "TRAPDOOR", "STAIRS", "SHULKER", "FLIGHT", "CREATIVE", "SLAB", "ILLEGAL_BLOCK", "TELEPORT", "FENCE", "JUMP_BOOST", "HONEY", "FISHING_ROD", "WEB", "FULLY_STUCK", "SHULKER_BOX", "ELYTRA", "VEHICLE", "BED", "BLOCK_PLACE", "SLEEPING", "CANCELLED_BREAK", "ANVIL", "PISTON", "TURTLE_EGG", "DOOR", "DEATH", "PARTIALLY_STUCK", "WALL", "PLACED_WEB", "ENDER_PEARL", "LEVITATION", "BUBBLE_COLUMN", "FROZEN", "BLOCK_BREAK", "SWEET_BERRIES", "EMPTIED_BUCKET", "FARMLAND", "LENIENT_SCAFFOLDING", "ATTACK_DAMAGE", "SEAGRASS", "DRIPSTONE", "WATERLOGGED", "CHEST", "SOUL_SAND", "GLITCHED_BLOCKS_ABOVE", "POWDER_SNOW"};
    private static final double[] FALL = {0, -.07840000152587834, -.15523200451659847, -.23052736891296632, -.30431682745754074};

    void evaluate(Observation input, DetectionMemory memory, FindingSink sink) {
        if (input.kind() != Observation.Kind.MOVE || !input.state("position-packet")) return;
        if (MovementRuleInputs.outer(input, true, false)) sprint(input, sink);
        if (MovementRuleInputs.outer(input, true, true)) {
            strafe(input, memory, sink);
            clip(input, sink);
        }
    }

    private void sprint(Observation input, FindingSink sink) {
        if (!MovementRuleInputs.finite(input, "source-sprint-position-count", "source-client-ground-movement-age", "source-horizontal", "source-position-dx", "source-position-dz", "source-yaw")
                || MovementRuleInputs.bit(input, "source-swift-sneak") == null) return;
        double angle = MovementRuleInputs.angle(input.value("source-position-dx"), input.value("source-position-dz"), (float) input.value("source-yaw"));
        MovementRuleInputs.strict(input, sink, "sprint-a", input.value("source-sprint-position-count") > 10 && angle > 1.5
                && input.value("source-client-ground-movement-age") > 6 && input.value("source-horizontal") > .25 && input.value("source-swift-sneak") == 0,
                "sprinting-motion-angle-radians=" + angle, SPRINT);
    }

    private void strafe(Observation input, DetectionMemory memory, FindingSink sink) {
        if (!MovementRuleInputs.finite(input, "source-climbable-contact-position-age") || input.value("source-climbable-contact-position-age") < 100) return;
        if (!MovementRuleInputs.finite(input, "source-position-dx", "source-position-dz", "source-previous-position-dx", "source-previous-position-dz", "source-horizontal",
                "source-client-air-movement-age", "source-walk-position-count", "source-piston-position-age", "source-flight-direct-position-age",
                "source-velocity-position-age", "source-explosion-position-age", "protocol-air-drag")
                || MovementRuleInputs.bit(input, "source-sprinting") == null || MovementRuleInputs.bit(input, "protocol-server-at-least-26-1") == null) return;
        boolean sprinting = input.value("source-sprinting") == 1;
        double drag = MovementRuleInputs.drag(input, .9100000262260437);
        double x = input.value("source-position-dx") - input.value("source-previous-position-dx") * drag;
        double z = input.value("source-position-dz") - input.value("source-previous-position-dz") * drag;
        double residual = Math.sqrt(x * x + z * z) / (memory.get("movement.heading.previous-sprint") == 1 ? 1.3 : 1)
                - (sprinting || input.value("source-walk-position-count") < 2 ? .026 : .02);
        double maximum = .0075 + (input.value("source-flight-direct-position-age") < 5 ? .04 : 0);
        MovementRuleInputs.strict(input, sink, "strafe-a", residual > maximum && input.value("source-horizontal") > .25
                && input.value("source-client-air-movement-age") > 3 && input.value("source-piston-position-age") >= 30
                && input.value("source-velocity-position-age") >= 3 && input.value("source-explosion-position-age") >= 10,
                "horizontal-input-residual=" + residual + " limit=" + maximum, STRAFE);
        memory.set("movement.heading.previous-sprint", sprinting ? 1 : 0);
    }

    private void clip(Observation input, FindingSink sink) {
        if (!MovementRuleInputs.finite(input, "total-movement-packets", "protocol-gravity", "source-client-air-movement-age", "source-delta-y",
                "source-velocity-position-age", "source-all-damage-age") || input.value("total-movement-packets") < 30 || input.value("protocol-gravity") > .1) return;
        double age = input.value("source-client-air-movement-age");
        if (age <= 0 || age >= 5 || age != Math.rint(age)) return;
        double dy = input.value("source-delta-y"), difference = Math.abs(dy - FALL[(int) age]);
        if (dy < 0 && difference > .75 && input.value("source-velocity-position-age") >= 50 && input.value("source-all-damage-age") >= 20) {
            var excluded = ProtocolExemptions.evaluate(input, CLIP);
            if (excluded.known() && !excluded.active()) sink.direct("vclip-a", "fall-phase-error=" + difference + " phase=" + age);
        }
    }
}
