package io.github.lino9999.linoac.packet;

import io.github.lino9999.linoac.detection.combat.CombatMaterials;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class WorldMaterialFacts {
    private static final Set<String> ICE = Set.of("ICE", "PACKED_ICE", "BLUE_ICE", "FROSTED_ICE");
    private static final Set<String> KEYS = sample(java.util.Collections.nCopies(36, new ServerWorldSnapshot.Cell("AIR", false, true)), true, false).keySet();
    private WorldMaterialFacts() {}
    static Set<String> keys() { return KEYS; }

    static Map<String, Double> sample(List<ServerWorldSnapshot.Cell> cells, boolean twoBelowAir, boolean thirdBelowSlime) {
        if (cells.size() != 36) return Map.of();
        var names = cells.stream().map(ServerWorldSnapshot.Cell::material).toList();
        var combat = CombatMaterials.sample(names);
        var facts = new HashMap<String, Double>();
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
    private static boolean plane(List<ServerWorldSnapshot.Cell> cells, int lower, int upper, Predicate<ServerWorldSnapshot.Cell> predicate) {
        return java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 >= lower && i % 12 < upper).anyMatch(i -> predicate.test(cells.get(i)));
    }
    private static void put(Map<String, Double> facts, String key, boolean active) { facts.put(key, active ? 1d : 0d); }
}
