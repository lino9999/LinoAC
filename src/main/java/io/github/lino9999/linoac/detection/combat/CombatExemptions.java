package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

import java.util.HashSet;

public final class CombatExemptions {
    private static final String[] CRITICALS_A = {"LIQUID", "TRAPDOOR", "COLLIDING_VERTICALLY", "TELEPORT", "BOAT", "WEB", "ENDER_PEARL", "CHORUS_FRUIT", "ELYTRA", "SERVER_POSITION", "CARPET", "VEHICLE", "FARMLAND", "GLIDING", "SLAB"};
    private static final String[] CRITICALS_B = {"LIQUID", "TRAPDOOR", "COLLIDING_VERTICALLY", "BOAT", "TELEPORT", "WEB", "ENDER_PEARL", "CHORUS_FRUIT", "SERVER_POSITION", "VEHICLE", "FLIGHT", "SLAB", "STAIRS"};
    private static final String[] VELOCITY_A = {"COLLIDING_VERTICALLY", "LIQUID", "FROZEN", "CHUNK", "FALL_DAMAGE", "ENDER_PEARL", "WEB", "VOID", "SLOW_FALLING", "TRAPDOOR", "SOUL_SAND", "SLIME", "COMBO_MODE", "FLIGHT", "VEHICLE", "MYTHIC_MOB", "ELYTRA", "FENCE", "WALL", "FIREBALL", "SWEET_BERRIES", "PLACED_WEB"};
    private static final String[] VELOCITY_B = {"COLLIDING_VERTICALLY", "LIQUID", "FROZEN", "CHUNK", "FALL_DAMAGE", "ENDER_PEARL", "WEB", "VOID", "SLOW_FALLING", "PLACED_WEB", "TRAPDOOR", "SOUL_SAND", "SLIME", "COMBO_MODE", "FLIGHT", "NETHERITE_ARMOR", "VEHICLE", "WALL", "MYTHIC_MOB", "ELYTRA", "SWEET_BERRIES", "FENCE"};
    private static final String[] VELOCITY_D = {"SLOW_FALLING", "LEVITATION", "COLLIDING_VERTICALLY", "COLLIDING_HORIZONTALLY", "ELYTRA", "GLIDING", "RIPTIDE", "ENTITY_COLLISION", "SWEET_BERRIES", "LIQUID", "CLIMBABLE", "FALL_DAMAGE", "ICE", "SOUL_SAND", "HONEY", "CAKE", "NETHERITE_ARMOR", "FROZEN", "CHUNK", "TRAPDOOR", "DOOR", "ILLEGAL_BLOCK", "VEHICLE", "DOLPHINS_GRACE", "MYTHIC_MOB", "PLACED_WEB", "VOID", "CAULDRON", "FENCE", "WEB", "WATERLOGGED", "ENDER_PEARL", "FISHING_ROD", "RESPAWN", "DRAGON_DAMAGE", "TELEPORT", "DEATH", "WORLD_CHANGE", "FLIGHT"};

    public Observation update(Observation input, VelocityContext.Snapshot velocity) {
        var states = new HashSet<>(input.states());
        for (String id : new String[]{"criticals-a", "criticals-b", "velocity-a", "velocity-b", "velocity-d"}) {
            states.remove("source-" + id + "-known");
            states.remove("source-" + id + "-exempt");
            states.remove("source-" + id + "-early-return");
        }
        states.remove("source-velocity-d-input-known");
        states.remove("source-velocity-horizontal-collision");
        states.remove("source-netherite-armor");
        Boolean materials = binary(input, "source-criticals-a-material-exempt");
        var a = evaluate(input, CRITICALS_A);
        apply(states, "criticals-a", combine(a, materials));
        apply(states, "criticals-b", evaluate(input, CRITICALS_B));
        Boolean border = binary(input, "source-world-border");
        Boolean matchingPosition = binary(input, "source-server-position-match");
        Boolean body = binary(input, "source-body-obstructed");
        Boolean ceiling = binary(input, "source-ceiling-obstructed");
        Boolean armor = ProtocolExemptions.value(input, "NETHERITE_ARMOR");
        Boolean collision = ProtocolExemptions.value(input, "COLLIDING_HORIZONTALLY");
        if (Boolean.TRUE.equals(armor)) states.add("source-netherite-armor");
        if (Boolean.TRUE.equals(collision)) states.add("source-velocity-horizontal-collision");
        Boolean outer = either(border, matchingPosition);
        var va = evaluate(input, VELOCITY_A);
        if (outer != null && velocity.complete() && (va.active() || armor != null && collision != null)) apply(states, "velocity-a", va);
        if (!Boolean.FALSE.equals(outer)) states.add("source-velocity-a-early-return");
        Boolean bEarly = either(border, either(body, ceiling));
        if (velocity.complete() && (!velocity.velocityPending() || velocity.pendingVelocities() > 0)) bEarly = true;
        if (bEarly != null && velocity.complete()) apply(states, "velocity-b", evaluate(input, VELOCITY_B));
        if (!Boolean.FALSE.equals(bEarly)) states.add("source-velocity-b-early-return");
        var vd = evaluate(input, VELOCITY_D);
        if (outer != null && velocity.complete() && vd.known()) {
            states.add("source-velocity-d-input-known");
            if (vd.active()) states.add("source-velocity-d-exempt");
        }
        if (!Boolean.FALSE.equals(outer)) states.add("source-velocity-d-early-return");
        return new Observation(input.kind(), input.tick(), input.nanos(), input.values(), states);
    }

    public static ProtocolExemptions.Result evaluate(Observation input, String... names) {
        return ProtocolExemptions.evaluate(input, names);
    }

    private void apply(HashSet<String> states, String id, ProtocolExemptions.Result result) {
        if (result.known()) states.add("source-" + id + "-known");
        if (result.active()) states.add("source-" + id + "-exempt");
    }

    private ProtocolExemptions.Result combine(ProtocolExemptions.Result result, Boolean extra) {
        return new ProtocolExemptions.Result(result.active() || Boolean.TRUE.equals(extra) || result.known() && extra != null,
                result.active() || Boolean.TRUE.equals(extra));
    }

    private Boolean binary(Observation input, String key) {
        if (!input.has(key)) return null;
        if (input.value(key) == 0) return Boolean.FALSE;
        if (input.value(key) == 1) return Boolean.TRUE;
        return null;
    }

    private Boolean either(Boolean first, Boolean second) {
        if (Boolean.TRUE.equals(first) || Boolean.TRUE.equals(second)) return Boolean.TRUE;
        if (first == null || second == null) return null;
        return Boolean.FALSE;
    }
}
