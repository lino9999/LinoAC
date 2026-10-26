package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;
import java.util.Map;
import java.util.Set;

final class MovementExemptions {
    private static final Map<String, String[]> RULES = Map.ofEntries(
            Map.entry("motion-a", new String[] {
                    "HALF_BLOCK", "CLIMBABLE", "FLIGHT", "BED", "LIQUID", "TELEPORT",
                    "VELOCITY", "ILLEGAL_BLOCK", "SLEEPING", "SOUL_SAND", "HONEY", "SLIME",
                    "LEVITATION", "SPECTATOR", "SCAFFOLDING", "FENCE", "BUBBLE_COLUMN", "CREATIVE",
                    "SHULKER", "SHULKER_BOX", "ELYTRA", "GLIDING", "CANCELLED_PLACE", "LENIENT_SCAFFOLDING",
                    "PLACED_CLIMBABLE", "KELP", "BOAT", "CHUNK", "TRAPDOOR", "POWDER_SNOW",
                    "EMPTIED_BUCKET", "CAKE", "WEB"
            }),
            Map.entry("motion-b", new String[] {
                    "WEB", "LILY_PAD", "JOINED", "ELYTRA", "SIGN", "ANVIL",
                    "SHULKER", "SHULKER_BOX", "PLACED_WEB", "TELEPORT", "RIPTIDE", "CREATIVE",
                    "SPECTATOR", "BOAT", "WORLD_CHANGE", "GLIDING", "ENDER_PEARL", "LIQUID",
                    "CHORUS_FRUIT", "TRAPDOOR", "CHUNK", "PISTON", "ILLEGAL_BLOCK", "VELOCITY",
                    "LENIENT_SCAFFOLDING", "CHORUS_FRUIT", "FARMLAND", "COLLIDING_VERTICALLY", "TRAPDOOR", "STAIRS",
                    "SLAB", "SLOW_FALLING", "BED"
            }),
            Map.entry("motion-c", new String[] {
                    "JUMP_BOOST", "VELOCITY", "FLIGHT", "ILLEGAL_BLOCK", "CREATIVE", "SLIME",
                    "GLIDING", "WEB", "VEHICLE", "TELEPORT", "SCAFFOLDING", "LIQUID",
                    "SLAB", "STAIRS", "ELYTRA"
            }),
            Map.entry("motion-e", new String[] {
                    "TELEPORT", "RIPTIDE", "GLIDING", "STAIRS", "VELOCITY", "JOINED",
                    "FLIGHT", "CREATIVE", "VEHICLE", "WORLD_CHANGE", "DOLPHINS_GRACE", "SOUL_SPEED",
                    "SLIME", "HONEY", "DEATH", "RIPTIDE", "SLEEPING", "LEVITATION",
                    "LIQUID", "ANVIL", "SERVER_POSITION", "PISTON", "WEB"
            }),
            Map.entry("step-a", new String[] {
                    "JOINED", "TELEPORT", "SHULKER_BOX", "SPECTATOR", "WORLD_CHANGE", "RIPTIDE",
                    "BOAT", "VEHICLE", "PISTON", "SHULKER", "ENDER_PEARL", "SLEEPING",
                    "BED", "BLOCK_BREAK"
            }),
            Map.entry("step-c", new String[] {
                    "TELEPORT", "WORLD_CHANGE", "DOOR", "DRAGON_DAMAGE", "DEATH", "JOINED",
                    "FLIGHT", "CREATIVE", "GLIDING", "EXPLOSION", "CANCELLED_PLACE", "ELYTRA",
                    "LIQUID", "VEHICLE", "RIPTIDE", "FISHING_ROD", "SLEEPING", "BLOCK_PLACE",
                    "VELOCITY", "FAST", "GLIDING", "SLIME", "ENDER_PEARL", "ANVIL",
                    "END_ROD", "CHAIN", "DRIPSTONE", "SERVER_POSITION", "PISTON", "JUMP_BOOST",
                    "CLIMBABLE", "SCAFFOLDING", "POWDER_SNOW", "SNOW", "SLOW_FALLING"
            }),
            Map.entry("jump-a", new String[] {
                    "VELOCITY", "COLLIDING_VERTICALLY", "HOPPER", "SLIME", "SCAFFOLDING", "BOAT",
                    "TRAPDOOR", "STAIRS", "SHULKER", "FLIGHT", "CREATIVE", "SLAB",
                    "ILLEGAL_BLOCK", "JUMP_BOOST_RAN_OUT", "FENCE", "JUMP_BOOST", "HONEY", "FISHING_ROD",
                    "WEB", "FULLY_STUCK", "SHULKER_BOX", "VEHICLE", "BED", "SEA_PICKLE",
                    "CHAIN", "SLEEPING", "CANCELLED_BREAK", "ANVIL", "PISTON", "TURTLE_EGG",
                    "DOOR", "DEATH", "PARTIALLY_STUCK", "WALL", "PLACED_WEB", "BUBBLE_COLUMN",
                    "FROZEN", "KELP", "SNOW", "BLOCK_BREAK", "SWEET_BERRIES", "EMPTIED_BUCKET",
                    "CONDUIT", "FARMLAND", "LENIENT_SCAFFOLDING", "ATTACK_DAMAGE", "SEAGRASS", "DRIPSTONE",
                    "WATERLOGGED", "CHEST", "SOUL_SAND", "GLITCHED_BLOCKS_ABOVE", "POWDER_SNOW", "SIGN",
                    "SEA_PICKLE", "PACKET_EXPLOSION"
            }),
            Map.entry("jump-c", new String[] {
                    "VELOCITY", "COLLIDING_VERTICALLY", "HOPPER", "SLIME", "SCAFFOLDING", "BOAT",
                    "TRAPDOOR", "STAIRS", "SHULKER", "FLIGHT", "CREATIVE", "SLAB",
                    "ILLEGAL_BLOCK", "JUMP_BOOST_RAN_OUT", "FENCE", "JUMP_BOOST", "HONEY", "FISHING_ROD",
                    "WEB", "FULLY_STUCK", "SHULKER_BOX", "VEHICLE", "BED", "SEA_PICKLE",
                    "CHAIN", "SLEEPING", "CANCELLED_BREAK", "ANVIL", "PISTON", "TURTLE_EGG",
                    "DOOR", "DEATH", "PARTIALLY_STUCK", "WALL", "PLACED_WEB", "BUBBLE_COLUMN",
                    "FROZEN", "KELP", "BLOCK_BREAK", "SWEET_BERRIES", "EMPTIED_BUCKET", "SERVER_POSITION_FAST",
                    "FARMLAND", "LENIENT_SCAFFOLDING", "ATTACK_DAMAGE", "SEAGRASS", "DRIPSTONE", "WATERLOGGED",
                    "CHEST", "SOUL_SAND", "GLITCHED_BLOCKS_ABOVE", "POWDER_SNOW", "SIGN", "SEA_PICKLE"
            }),
            Map.entry("jump-b", new String[] {
                    "JOINED", "TELEPORT", "RIPTIDE", "VEHICLE", "HONEY", "GLIDING",
                    "LEVITATION", "SLAB", "FLIGHT", "JUMP_BOOST_RAN_OUT", "BOAT", "BUBBLE_COLUMN",
                    "WALL", "CREATIVE", "SHULKER_BOX", "ANVIL", "SLIME", "PISTON",
                    "TURTLE_EGG", "DOOR", "DEATH", "ENDER_PEARL", "PLACED_SLIME", "CHORUS_FRUIT",
                    "MYTHIC_MOB", "BLOCK_BREAK", "FISHING_ROD", "SLEEPING", "SHULKER"
            }),
            Map.entry("speed-a", new String[] {
                    "ILLEGAL_BLOCK", "FULLY_STUCK", "DOLPHINS_GRACE", "WEB", "SLEEPING", "GLIDING",
                    "ELYTRA", "DEPTH_STRIDER", "VOID", "WORLD_CHANGE", "CHUNK", "JOINED",
                    "ENTITY_COLLISION", "ANVIL", "TRAPDOOR", "SPECTATOR", "PARTIALLY_STUCK", "FENCE_GATE",
                    "FROZEN", "CHORUS_FRUIT", "BLOCK_BREAK", "CANCELLED_MOVE"
            }),
            Map.entry("flight-a", new String[] {
                    "SHULKER_BOX", "WEB", "BOAT", "ILLEGAL_BLOCK", "SLEEPING", "SLOW_FALLING",
                    "SHULKER", "TRAPDOOR", "FISHING_ROD", "LILY_PAD", "SPECTATOR", "HONEY",
                    "CHORUS_FRUIT", "BED", "CHAIN", "WORLD_CHANGE", "CANCELLED_PLACE", "GLIDING",
                    "ELYTRA", "LAGGED_NEAR_GROUND_MODERN", "EMPTIED_BUCKET", "DEAD", "CANCELLED_BREAK", "FENCE",
                    "LAGGED_NEAR_GROUND", "BLOCK_PLACE_FAST", "GLASS_PANE", "NOT_MOVING"
            }),
            Map.entry("flight-b", new String[] {
                    "BOAT", "PARTIALLY_STUCK", "DEAD", "CHAIN", "WEB", "PLACING",
                    "WATERLOGGED", "SLIME", "WORLD_CHANGE", "HONEY", "FISHING_ROD", "EMPTIED_BUCKET",
                    "SWEET_BERRIES", "BUBBLE_COLUMN", "SCAFFOLDING", "NOT_MOVING", "SEAGRASS", "SIGN",
                    "ANVIL", "END_ROD", "SPECTATOR", "SHULKER", "SHULKER_BOX", "FENCE",
                    "KELP", "BLOCK_BREAK", "FENCE_GATE", "JOINED_CHUNK_LOAD", "DOLPHINS_GRACE", "PLACED_WEB",
                    "SEAGRASS", "WALL", "SEA_PICKLE", "CHEST", "DRIPSTONE", "POWDER_SNOW",
                    "FULLY_STUCK", "GLIDING", "ELYTRA", "BED", "SLEEPING", "CANCELLED_MOVE",
                    "SIGN", "CONDUIT"
            }),
            Map.entry("flight-c", new String[] {
                    "SHULKER_BOX", "BOAT", "WORLD_CHANGE", "JUMP_BOOST", "FENCE", "LAGGED_NEAR_GROUND",
                    "ILLEGAL_BLOCK", "SHULKER", "FROZEN", "FISHING_ROD", "CANCELLED_PLACE", "NOT_MOVING",
                    "SPECTATOR", "CHORUS_FRUIT", "PISTON", "NEAR_GROUND", "LAGGED_NEAR_GROUND_MODERN", "EMPTIED_BUCKET",
                    "GLIDING", "ELYTRA", "BOAT", "PLACED_SLIME", "DEAD"
            }),
            Map.entry("flight-d", new String[] {
                    "LAGGED_NEAR_GROUND", "DEAD", "WEB", "JUMP_BOOST_RAN_OUT", "EMPTIED_BUCKET", "WORLD_CHANGE",
                    "ILLEGAL_BLOCK", "BOAT", "LIQUID", "NOT_MOVING", "FENCE", "SHULKER",
                    "TRAPDOOR", "SHULKER_BOX", "NEAR_GROUND", "FISHING_ROD", "LAGGED_NEAR_GROUND_MODERN", "VOID",
                    "CANCELLED_PLACE", "SPECTATOR", "SERVER_POSITION", "PISTON", "HIGH_JUMP_BOOST", "NEAR_GROUND",
                    "SLIME", "PARTIALLY_STUCK", "FULLY_STUCK", "RESPAWN", "PLACED_SLIME", "GLIDING",
                    "ELYTRA"
            }),
            Map.entry("flight-e", new String[] {
                    "BED", "EMPTIED_BUCKET", "WEB", "LAGGED_NEAR_GROUND", "SLEEPING", "JOINED_CHUNK_LOAD",
                    "ILLEGAL_BLOCK", "BOAT", "SHULKER", "BLOCK_PLACE", "LAGGED_NEAR_GROUND_MODERN", "TRAPDOOR",
                    "WORLD_CHANGE", "NOT_MOVING", "LILY_PAD", "SPECTATOR", "SHULKER_BOX", "GLIDING",
                    "ELYTRA", "HONEY", "CANCELLED_PLACE", "FENCE", "SWEET_BERRIES", "DEAD"
            }),
            Map.entry("flight-f", new String[] { "FLIGHT", "LIQUID", "WALL", "CONDUIT", "FARMLAND", "SLOW_FALLING", "LEVITATION", "RIPTIDE", "WEB", "WATERLOGGED", "SHULKER_BOX", "SPECTATOR", "SLEEPING", "FISHING_ROD", "SLIME", "AROUND_SLIME", "CHUNK", "PLACED_SLIME", "POWDER_SNOW", "SEAGRASS", "SCAFFOLDING", "CLIMBABLE", "SWIMMING", "SHULKER", "SIGN", "JOINED_CHUNK_LOAD", "EMPTIED_BUCKET", "SWEET_BERRIES", "BOAT", "HONEY", "SEA_PICKLE", "ENDER_PEARL", "TELEPORT", "BUBBLE_COLUMN", "VEHICLE", "CHEST", "PLACED_WEB", "KELP", "GLIDING", "GLITCHED_BLOCKS_ABOVE" }));

    private MovementExemptions() {}
    static Set<String> ids() { return RULES.keySet(); }
    static ProtocolExemptions.Result evaluate(String id, Observation input) {
        return ProtocolExemptions.evaluate(input, RULES.get(id));
    }
    static ProtocolExemptions.Result including(String id, Observation input, String... extraKeys) {
        var result = evaluate(id, input);
        boolean known = result.known();
        if (result.active()) return result;
        for (String key : extraKeys) {
            Boolean value = bit(input, key);
            if (Boolean.TRUE.equals(value)) return new ProtocolExemptions.Result(true, true);
            if (value == null) known = false;
        }
        return new ProtocolExemptions.Result(known, false);
    }
    static Boolean bit(Observation input, String key) {
        return !input.has(key) ? null : input.value(key) == 0 ? Boolean.FALSE : input.value(key) == 1 ? Boolean.TRUE : null;
    }
}
