package io.github.lino9999.linoac.packet;

import io.github.lino9999.linoac.detection.combat.CombatMaterials;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class WorldMaterialFacts {
    private static final Set<String> ICE = Set.of("ICE", "PACKED_ICE", "BLUE_ICE", "FROSTED_ICE");
    private static final Set<String> KEYS = sample(java.util.Collections.nCopies(36, new ServerWorldSnapshot.Cell("AIR", false, true)), true, false, true, true, "AIR").keySet();
    private WorldMaterialFacts() {}
    static Set<String> keys() { return KEYS; }

    static Map<String, Double> sample(List<ServerWorldSnapshot.Cell> cells, boolean twoBelowAir, boolean thirdBelowSlime) {
        return sample(cells, twoBelowAir, thirdBelowSlime, null, null);
    }

    static Map<String, Double> sample(List<ServerWorldSnapshot.Cell> cells, boolean twoBelowAir, boolean thirdBelowSlime, Boolean lowerPlaneAir, Boolean thirdBelowAir) {
        return sample(cells, twoBelowAir, thirdBelowSlime, lowerPlaneAir, thirdBelowAir, null);
    }
    static Map<String, Double> sample(List<ServerWorldSnapshot.Cell> cells, boolean twoBelowAir, boolean thirdBelowSlime, Boolean lowerPlaneAir, Boolean thirdBelowAir, String secondBelowMaterial) {
        if (cells.size() != 36) return Map.of();
        var names = cells.stream().map(ServerWorldSnapshot.Cell::material).toList();
        var combat = CombatMaterials.sample(names);
        var facts = new HashMap<String, Double>();
        put(facts, "source-liquid-surface", !plane(cells, 0, 3, cell -> !liquid(cell.material())));
        put(facts, "protocol-exempt-FULLY_SUBMERGED", liquid(cells.get(16).material()) && liquid(cells.get(19).material()));
        put(facts, "protocol-exempt-NEAR_SOLID", cells.stream().anyMatch(ServerWorldSnapshot.Cell::solid));
        put(facts, "source-below-one-air", cells.get(13).air());
        put(facts, "source-central-feet-air", cells.get(16).air());
        put(facts, "source-central-feet-soul-sand", cells.get(16).material().equals("SOUL_SAND"));
        put(facts, "source-central-body-web", cells.get(16).material().equals("COBWEB") || cells.get(19).material().equals("COBWEB"));
        put(facts, "source-climb-ready", climbable(cells.get(16).material()) && climbable(cells.get(19).material()));
        if (secondBelowMaterial != null) put(facts, "source-climb-materials", climbable(cells.get(13).material()) && climbable(secondBelowMaterial));
        put(facts, "source-wallclimb-ready", plane(cells, 9, 12, cell -> !cell.air()));
        facts.put("source-wall-air-count-a", (double) java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 >= 9 && cells.get(i).air()).count());
        facts.put("source-wall-air-count-b", (double) java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 < 3 && cells.get(i).air()).count());
        put(facts, "source-dripleaf-contact", any(names, name -> name.contains("DRIP") && name.contains("LEAF")));
        put(facts, "source-sculk-sensor-contact", names.contains("SCULK_SENSOR"));
        put(facts, "source-door-contact", any(names, name -> name.contains("DOOR")));
        put(facts, "source-liquid-contact", any(names, name -> Set.of("WATER", "LAVA", "BUBBLE_COLUMN").contains(name))
                || liquid(cells.get(16).material()) || liquid(cells.get(19).material()));
        put(facts, "protocol-air-volume27", java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 < 9).allMatch(i -> cells.get(i).air()));
        put(facts, "protocol-solid-center", cells.get(16).solid() && cells.get(19).solid());
        put(facts, "source-body-obstructed", plane(cells, 3, 9, cell -> !cell.air()));
        put(facts, "source-ceiling-obstructed", plane(cells, 9, 12, cell -> !cell.air()));
        put(facts, "source-soul-sand", any(names, name -> name.equals("SOUL_SAND") || name.equals("SOUL_SOIL")));
        put(facts, "source-bubble-column", names.contains("BUBBLE_COLUMN"));
        put(facts, "source-slime-contact", names.contains("SLIME_BLOCK"));
        put(facts, "source-slime-below", thirdBelowSlime);
        put(facts, "source-ice-below-contact", plane(cells, 0, 3, cell -> ICE.contains(cell.material())));
        put(facts, "source-trapdoor-contact", combat.trapdoor());
        put(facts, "source-fence-contact", any(names, name -> name.contains("FENCE")));
        put(facts, "source-bed-contact", any(names, name -> name.contains("BED")));
        put(facts, "source-body-slime-contact", plane(cells, 3, 9, cell -> cell.material().equals("SLIME_BLOCK")));
        put(facts, "source-body-slab-contact", plane(cells, 3, 9, cell -> cell.material().contains("SLAB") || cell.material().contains("STEP")));
        put(facts, "source-stairs-contact", any(names, name -> name.contains("STAIR")));
        put(facts, "source-slab-contact", any(names, name -> name.contains("SLAB") || name.contains("STEP")));
        put(facts, "source-powder-snow-contact", names.contains("POWDER_SNOW"));
        put(facts, "source-cobweb-contact", names.contains("COBWEB"));
        put(facts, "source-heavy-core-contact", names.contains("HEAVY_CORE"));
        put(facts, "source-flower-pot-contact", names.contains("FLOWER_POT"));
        put(facts, "source-lectern-contact", names.contains("LECTERN"));
        put(facts, "source-repeater-contact", names.contains("REPEATER"));
        put(facts, "source-bed-surface-contact", any(names, name -> name.contains("BED") && !name.equals("BEDROCK")));
        put(facts, "source-daylight-detector-contact", names.contains("DAYLIGHT_DETECTOR"));
        put(facts, "source-lantern-contact", any(names, name -> name.contains("LANTERN")));
        put(facts, "source-chain-contact", names.contains("CHAIN"));
        put(facts, "source-cake-contact", names.contains("CAKE"));
        put(facts, "source-campfire-contact", any(names, name -> name.equals("CAMPFIRE") || name.equals("SOUL_CAMPFIRE")));
        put(facts, "source-snow-contact", names.contains("SNOW"));
        put(facts, "source-amethyst-contact", any(names, name -> name.contains("AMETHYST")));
        put(facts, "source-candle-contact", any(names, name -> name.contains("CANDLE")));
        put(facts, "source-dripstone-contact", any(names, name -> name.contains("DRIPSTONE")));
        put(facts, "source-dried-ghast-contact", names.contains("DRIED_GHAST"));
        put(facts, "source-ceiling-slab-contact", plane(cells, 9, 12, cell -> cell.material().contains("SLAB") || cell.material().contains("STEP")));
        put(facts, "source-cached-solid-footprint", !cells.get(13).air() || !twoBelowAir);
        put(facts, "source-center-support-solid", cells.get(13).solid() && !liquid(cells.get(13).material()));
        if (lowerPlaneAir != null) put(facts, "source-lower-footprint-air", lowerPlaneAir && !plane(cells, 0, 3, cell -> !cell.air()));
        if (thirdBelowAir != null) put(facts, "source-three-below-obstructed", !cells.get(13).air() || !twoBelowAir || !thirdBelowAir);
        put(facts, "source-upper-body-obstructed", plane(cells, 6, 9, cell -> !cell.air()));
        put(facts, "source-air-counter-reset-material", any(names, name -> name.contains("TRAP") && name.contains("DOOR") || name.contains("CARPET")
                || Set.of("CAMPFIRE", "SOUL_CAMPFIRE", "BREWING_STAND", "REPEATER", "DAYLIGHT_DETECTOR", "LILY_PAD", "FLOWER_POT").contains(name)
                || name.contains("SKULL") || name.contains("HEAD") || name.contains("SHULKER")));
        put(facts, "source-honey-contact", combat.honey());
        put(facts, "source-ice-contact", combat.ice());
        put(facts, "source-shulker-box-contact", any(names, name -> name.contains("SHULKER")));
        put(facts, "source-scaffolding-contact", names.contains("SCAFFOLDING"));
        put(facts, "source-farmland-contact", any(names, name -> name.contains("FARMLAND")));
        put(facts, "source-piston-contact", any(names, name -> Set.of("PISTON", "PISTON_HEAD", "MOVING_PISTON", "STICKY_PISTON").contains(name)));
        put(facts, "source-criticals-a-material-exempt", combat.criticalMaterials());
        put(facts, "protocol-invalid-j-skip", any(names, name -> name.contains("DRIPSTONE") || name.equals("CHAIN") || name.contains("LANTERN")));
        put(facts, "protocol-invalid-j-exempt", any(names, name -> name.contains("SCULK") || name.equals("IRON_BARS") || name.equals("DAYLIGHT_DETECTOR")));
        put(facts, "protocol-exempt-BAMBOO", any(names, name -> name.contains("BAMBOO")));
        put(facts, "protocol-exempt-CHAIN", names.contains("CHAIN"));
        put(facts, "protocol-exempt-CHEST", any(names, name -> name.contains("CHEST")));
        put(facts, "protocol-exempt-CONDUIT", names.contains("CONDUIT"));
        put(facts, "protocol-exempt-DRIPSTONE", any(names, name -> name.contains("DRIPSTONE")));
        put(facts, "protocol-exempt-END_ROD", names.contains("END_ROD"));
        put(facts, "protocol-exempt-FENCE_GATE", any(names, name -> name.contains("GATE")));
        put(facts, "protocol-exempt-FULLY_STUCK", cells.get(16).solid() && cells.get(19).solid());
        put(facts, "protocol-exempt-PARTIALLY_STUCK", cells.get(16).solid() && cells.get(19).solid());
        put(facts, "protocol-exempt-GLASS_PANE", any(names, name -> name.contains("PANE")));
        put(facts, "protocol-exempt-GLITCHED_BLOCKS_ABOVE", plane(cells, 6, 9, cell -> !cell.air()));
        put(facts, "protocol-exempt-HALF_BLOCK", any(names, name -> name.contains("STAIR") || name.contains("SLAB") || name.contains("STEP")));
        put(facts, "protocol-exempt-HOPPER", names.contains("HOPPER"));
        put(facts, "protocol-exempt-KELP", names.contains("KELP") || names.contains("KELP_PLANT"));
        put(facts, "protocol-exempt-POWDER_SNOW", names.contains("POWDER_SNOW"));
        put(facts, "protocol-exempt-SEAGRASS", any(names, name -> name.contains("GRASS") && name.contains("SEA")));
        put(facts, "protocol-exempt-SEA_PICKLE", names.contains("SEA_PICKLE"));
        put(facts, "protocol-exempt-SIGN", any(names, name -> name.contains("SIGN")));
        put(facts, "protocol-exempt-PRESSURE_PLATE", any(names, name -> name.contains("PLATE")));
        put(facts, "protocol-exempt-SNOW", names.contains("SNOW"));
        put(facts, "protocol-exempt-SKULL", any(names, name -> name.contains("SKULL") || name.contains("HEAD")));
        put(facts, "protocol-exempt-ANVIL", any(names, name -> Set.of("ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL").contains(name)));
        put(facts, "protocol-exempt-TURTLE_EGG", names.contains("TURTLE_EGG"));
        put(facts, "protocol-exempt-LILY_PAD", names.contains("LILY_PAD"));
        put(facts, "protocol-exempt-WALL", any(names, name -> name.contains("WALL") && !name.contains("SIGN")));
        put(facts, "protocol-exempt-CAKE", combat.cake());
        put(facts, "protocol-exempt-CARPET", combat.carpet());
        put(facts, "protocol-exempt-CAULDRON", combat.cauldron());
        put(facts, "protocol-exempt-DOOR", combat.door());
        put(facts, "protocol-exempt-SWEET_BERRIES", combat.sweetBerries());
        put(facts, "protocol-exempt-CLIMBABLE", combat.climbable());
        put(facts, "protocol-exempt-ILLEGAL_BLOCK", combat.illegalBlock());
        put(facts, "protocol-exempt-SOUL_SAND", facts.get("source-soul-sand") == 1);
        put(facts, "protocol-exempt-COLLIDING_HORIZONTALLY", facts.get("source-body-obstructed") == 1);
        put(facts, "protocol-exempt-NEAR_GROUND", !cells.get(13).air() || !twoBelowAir);
        return Map.copyOf(facts);
    }

    private static boolean any(List<String> names, Predicate<String> predicate) { return names.stream().anyMatch(predicate); }
    private static boolean liquid(String name) { return name.contains("WATER") || name.contains("LAVA") || name.contains("BUBBLE"); }
    private static boolean climbable(String name) { return name.equals("LADDER") || name.contains("VINE") || name.equals("SCAFFOLDING"); }
    private static boolean plane(List<ServerWorldSnapshot.Cell> cells, int lower, int upper, Predicate<ServerWorldSnapshot.Cell> predicate) {
        return java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 >= lower && i % 12 < upper).anyMatch(i -> predicate.test(cells.get(i)));
    }
    private static void put(Map<String, Double> facts, String key, boolean active) { facts.put(key, active ? 1d : 0d); }
}
