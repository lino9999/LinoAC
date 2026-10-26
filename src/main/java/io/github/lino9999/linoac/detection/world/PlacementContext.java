package io.github.lino9999.linoac.detection.world;

import io.github.lino9999.linoac.detection.Observation;

public final class PlacementContext {
    private PlacementContext() {}

    public static java.util.Set<String> materialStates(String material) {
        var states = new java.util.HashSet<String>();
        states.add("world-material-known");
        if (material.contains("SLAB") || material.contains("STEP")) states.add("world-slab-material");
        if (material.equals("SCAFFOLDING")) states.add("world-scaffolding-material");
        if (material.contains("SLAB") || material.contains("STEP") || material.contains("SAND") || material.contains("STONE")
                || material.contains("GRAVEL") || material.contains("PATH") || material.contains("ANCHOR") || material.contains("CONCRETE")
                || material.contains("POWDER") || material.contains("STRIPPED") || material.contains("FARMLAND")
                || java.util.Set.of("ANVIL", "DAMAGED_ANVIL", "CHIPPED_ANVIL", "TNT", "GLOWSTONE").contains(material)) states.add("world-airplace-material-exempt");
        if (material.contains("TURTLE") || material.contains("EGG") || material.contains("SLAB") || material.contains("STEP")
                || material.contains("CONCRETE") || material.contains("POWDER") || material.equals("TNT")) states.add("world-face-material-exempt");
        if (material.equals("SCAFFOLDING") || material.contains("WIRE") || material.contains("STRING") || material.contains("TURTLE")
                || material.contains("EGG") || material.contains("LADDER") || material.contains("VINE")) states.add("world-tower-material-exempt");
        return java.util.Set.copyOf(states);
    }

    public static boolean fastPlaceMaterialExempt(String material) {
        return material.equals("SCAFFOLDING") || material.contains("SCULK");
    }

    public static boolean bridgeMaterialExempt(String material) {
        return material.contains("PATH") || material.contains("FARMLAND") || material.contains("GRAVEL")
                || material.contains("SAND") && !material.contains("STONE")
                || material.contains("CONCRETE") && material.contains("POWDER");
    }

    public static boolean scaffoldNMaterialExempt(String material) {
        return material.equals("SCAFFOLDING") || material.contains("SLAB") || material.contains("STEP") || material.contains("STAIR");
    }

    public static Boolean bridge(Observation observation) {
        if (!observation.state("bridge-material-known")) return null;
        if (!observation.state("placed-solid") || observation.state("bridge-material-exempt")) return false;
        if (!observation.state("block-two-below-known") || !observation.hasAll("bridge-air-samples", "against-x", "against-y", "against-z", "x", "y", "z", "block-face")) return null;
        for (String key : new String[]{"bridge-air-samples", "against-x", "against-y", "against-z", "x", "y", "z", "block-face"}) {
            if (!Double.isFinite(observation.value(key))) return null;
        }
        double air = observation.value("bridge-air-samples");
        if (air < 0 || air > 9 || air != Math.rint(air)) return null;
        double face = observation.value("block-face");
        if (face < 0 || face > 6 || face != Math.rint(face)) return false;
        return face != 1 && observation.state("block-two-below-air") && air > 6
                && observation.value("against-y") < observation.value("y")
                && Math.hypot(observation.value("against-x") - observation.value("x"), observation.value("against-z") - observation.value("z")) < 2.25;
    }
}
