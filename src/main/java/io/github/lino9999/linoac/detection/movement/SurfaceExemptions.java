package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;
import java.util.Map;
import java.util.Set;

final class SurfaceExemptions {
    private static final Map<String, String[]> RULES = Map.ofEntries(
            Map.entry("antilevitation-a", new String[] {
                    "SCAFFOLDING", "VOID", "COLLIDING_VERTICALLY", "TRAPDOOR", "BED", "SLIME",
                    "HONEY", "VELOCITY", "RIPTIDE", "JOINED", "SWIMMING", "CHUNK",
                    "JOINED_CHUNK_LOAD", "WEB", "SLEEPING", "DEATH", "FROZEN", "BLOCK_PLACE",
                    "BLOCK_BREAK", "FULLY_STUCK", "PARTIALLY_STUCK", "WORLD_CHANGE", "VEHICLE", "BOAT",
                    "GLIDING", "SLOW_FALLING", "BUBBLE_COLUMN", "SPECTATOR", "DOLPHINS_GRACE", "SHULKER_BOX",
                    "FENCE", "FENCE_GATE", "STAIRS"
            }),
            Map.entry("fastclimb-a", new String[] {
                    "JUMP_BOOST", "FLIGHT", "CREATIVE", "GLIDING", "LEVITATION", "SLEEPING",
                    "ENDER_PEARL", "TELEPORT", "WORLD_CHANGE", "DEATH", "SCAFFOLDING", "LIQUID",
                    "WATERLOGGED", "BLOCK_PLACE", "RIPTIDE", "LIQUID", "BUBBLE_COLUMN", "SLIME",
                    "PROJECTILE_DAMAGE", "WALL"
            }),
            Map.entry("wallclimb-a", new String[] {
                    "CLIMBABLE", "SCAFFOLDING", "JUMP_BOOST", "ELYTRA", "SNOW", "BLOCK_PLACE",
                    "FLIGHT", "CREATIVE", "GLIDING", "LEVITATION", "DEAD", "HAPPY_GHAST",
                    "VELOCITY", "RIPTIDE", "CANCELLED_PLACE", "NEAR_GROUND", "POWDER_SNOW", "BOAT",
                    "SLIME", "LIQUID", "END_ROD", "SHULKER", "SHULKER_BOX", "FISHING_ROD",
                    "FENCE", "WALL", "PACKET_EXPLOSION", "KELP", "CARPET", "BUBBLE_COLUMN",
                    "WATERLOGGED", "EMPTIED_BUCKET", "TRAPDOOR"
            }),
            Map.entry("jesus-a", new String[] {
                    "LILY_PAD", "STAIRS", "SLAB", "CAMPFIRE", "CARPET", "END_ROD",
                    "FENCE", "BOAT", "HAPPY_GHAST", "CANCELLED_PLACE", "CHAIN", "SNOW",
                    "SWIMMING_JESUS", "BED", "TRAPDOOR", "BOAT", "DOLPHINS_GRACE", "FLOWER_POT",
                    "SHULKER", "SPECTATOR", "SCAFFOLDING", "SKULL", "SEA_PICKLE", "NEAR_SOLID",
                    "NOT_MOVING", "BLOCK_PLACE"
            }),
            Map.entry("jesus-b", new String[] {
                    "LILY_PAD", "STAIRS", "SLAB", "CAMPFIRE", "CARPET", "END_ROD",
                    "FENCE", "BOAT", "FLOWER_POT", "SHULKER", "CANCELLED_PLACE", "CHAIN",
                    "SNOW", "SWIMMING_JESUS", "BED", "TRAPDOOR", "BOAT", "FULLY_SUBMERGED",
                    "BUBBLE_COLUMN", "VELOCITY", "DEPTH_STRIDER", "DOLPHINS_GRACE", "SEA_PICKLE", "SWIMMING_ON_OLD_VERSION",
                    "SPECTATOR", "SOUL_SAND", "NOT_MOVING", "NEAR_SOLID", "CHUNK", "SKULL",
                    "CLIMBABLE", "HAPPY_GHAST", "SCAFFOLDING", "SLOW_FALLING"
            }),
            Map.entry("jesus-c", new String[] {
                    "LILY_PAD", "STAIRS", "SLAB", "CAMPFIRE", "CARPET", "END_ROD",
                    "FENCE", "BOAT", "WEB", "HAPPY_GHAST", "CANCELLED_PLACE", "CHAIN",
                    "SNOW", "SWIMMING_JESUS", "BED", "TRAPDOOR", "BOAT", "FULLY_SUBMERGED",
                    "VELOCITY", "BUBBLE_COLUMN", "SWIMMING_ON_OLD_VERSION", "SOUL_SAND", "CLIMBABLE", "SPECTATOR",
                    "NEAR_SOLID", "SKULL", "FLOWER_POT", "CHUNK", "NOT_MOVING", "SCAFFOLDING"
            }),
            Map.entry("jesus-d", new String[] {
                    "LILY_PAD", "STAIRS", "SLAB", "CAMPFIRE", "CARPET", "END_ROD",
                    "FENCE", "BOAT", "NOT_MOVING", "CANCELLED_PLACE", "CHAIN", "SNOW",
                    "SWIMMING_JESUS", "BED", "TRAPDOOR", "BOAT", "FULLY_SUBMERGED", "VELOCITY",
                    "HAPPY_GHAST", "BUBBLE_COLUMN", "SWIMMING_ON_OLD_VERSION", "SPECTATOR", "CLIMBABLE", "NEAR_SOLID",
                    "SLIME", "HONEY", "CHUNK", "SCAFFOLDING", "SKULL"
            }),
            Map.entry("jesus-e", new String[] {
                    "LILY_PAD", "STAIRS", "SLAB", "CAMPFIRE", "CARPET", "END_ROD",
                    "FENCE", "FLOWER_POT", "HAPPY_GHAST", "NOT_MOVING", "CANCELLED_PLACE", "CHAIN",
                    "SNOW", "SWIMMING_JESUS", "BED", "TRAPDOOR", "BOAT", "FULLY_SUBMERGED",
                    "BUBBLE_COLUMN", "VELOCITY", "DEPTH_STRIDER", "DOLPHINS_GRACE", "FENCE", "FENCE_GATE",
                    "SWIMMING_ON_OLD_VERSION", "SPECTATOR", "BLOCK_PLACE", "CLIMBABLE", "NEAR_SOLID", "SLOW_FALLING",
                    "CHUNK", "WALL", "SCAFFOLDING", "SLIME", "SKULL"
            }),
            Map.entry("noslow-b", new String[] {
                    "FLIGHT", "CREATIVE", "VELOCITY", "TELEPORT", "ENDER_PEARL", "CHORUS_FRUIT",
                    "SOUL_SPEED", "ELYTRA", "RIPTIDE", "HIGH_SPEED", "DEPTH_STRIDER"
            }),
            Map.entry("noslow-c", new String[] {
                    "FLIGHT", "CREATIVE", "VELOCITY", "TELEPORT", "ENDER_PEARL", "CHORUS_FRUIT",
                    "RIPTIDE", "ELYTRA", "GLIDING", "DEPTH_STRIDER", "SERVER_POSITION_FAST", "ELYTRA",
                    "GLIDING", "SWIMMING", "TRAPDOOR"
            }));

    private SurfaceExemptions() {}
    static Set<String> ids() { return RULES.keySet(); }
    static ProtocolExemptions.Result evaluate(String id, Observation input, boolean includePrivate) {
        var result = ProtocolExemptions.evaluate(input, RULES.get(id));
        if (!includePrivate || result.active()) return result;
        String key = "source-movement-" + id + "-private-exempt";
        if (!input.has(key) || input.value(key) != 0 && input.value(key) != 1) return new ProtocolExemptions.Result(false, false);
        return input.value(key) == 1 ? new ProtocolExemptions.Result(true, true) : result;
    }
}
