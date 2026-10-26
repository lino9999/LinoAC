package io.github.lino9999.linoac.detection.combat;

import java.util.List;

public final class CombatMaterials {
    private CombatMaterials() {}

    public static Sample sample(List<String> materials) {
        if (materials == null || materials.size() != 36) return null;
        boolean cake = false, carpet = false, cauldron = false, door = false, sweetBerries = false;
        boolean honey = false, ice = false, climbable = false, trapdoor = false, illegalBlock = false, criticalMaterials = false;
        for (String material : materials) {
            if (material == null || material.isEmpty()) return null;
            boolean hasCarpet = material.contains("CARPET");
            boolean wall = material.contains("WALL") && !material.contains("SIGN");
            cake |= material.equals("CAKE");
            carpet |= hasCarpet;
            cauldron |= material.equals("CAULDRON");
            door |= material.contains("DOOR");
            sweetBerries |= material.equals("SWEET_BERRY_BUSH");
            honey |= material.equals("HONEY_BLOCK");
            ice |= material.equals("ICE") || material.equals("PACKED_ICE") || material.equals("FROSTED_ICE") || material.equals("BLUE_ICE");
            climbable |= material.equals("LADDER") || material.contains("VINE") || material.equals("SCAFFOLDING");
            trapdoor |= material.contains("TRAP") && material.contains("DOOR");
            illegalBlock |= material.equals("DAYLIGHT_DETECTOR") || material.equals("BREWING_STAND") || hasCarpet || wall
                    || material.contains("SKULL") || material.contains("HEAD") || material.equals("END_PORTAL_FRAME")
                    || material.equals("SCAFFOLDING") || material.equals("CAMPFIRE") || material.equals("SOUL_CAMPFIRE");
            criticalMaterials |= material.contains("LANTERN") || material.equals("FLOWER_POT") || material.contains("PATH") || material.equalsIgnoreCase("CACTUS");
        }
        return new Sample(cake, carpet, cauldron, door, sweetBerries, honey, ice, climbable, trapdoor, illegalBlock, criticalMaterials);
    }

    public record Sample(boolean cake, boolean carpet, boolean cauldron, boolean door, boolean sweetBerries,
                         boolean honey, boolean ice, boolean climbable, boolean trapdoor, boolean illegalBlock, boolean criticalMaterials) {}
}
