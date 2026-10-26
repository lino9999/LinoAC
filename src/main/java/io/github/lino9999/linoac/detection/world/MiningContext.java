package io.github.lino9999.linoac.detection.world;

import java.util.Set;

public final class MiningContext {
    private static final Set<String> EXACT_EXEMPT = Set.of("TARGET", "WATER", "WATER_LILY", "NOTEBLOCK", "MANGROVE_ROOTS", "CHISELED_BOOKSHELF", "FIRE", "SULFUR",
            "AIR", "CAVE_AIR", "VOID_AIR");
    private static final String[] CONTAINS_EXEMPT = {"LAVA", "WATER", "BUBBLE", "DOOR", "TRAP", "SCULK", "AMETHYST", "SHULKER", "DEEPSLATE", "PISTON", "INFESTED", "CONCRETE", "MUSHROOM"};
    private long deadlineMillis;

    public void start(long nowMillis, long expectedMillis) {
        if (nowMillis < 0 || expectedMillis < 0) throw new IllegalArgumentException("Mining time must be nonnegative.");
        deadlineMillis = expectedMillis > Long.MAX_VALUE - nowMillis ? Long.MAX_VALUE : nowMillis + expectedMillis;
    }

    public double difference(long nowMillis) { return (double) nowMillis - deadlineMillis; }
    public long deadlineMillis() { return deadlineMillis; }

    public static boolean materialExempt(String material) {
        if (material == null || EXACT_EXEMPT.contains(material)) return true;
        for (String fragment : CONTAINS_EXEMPT) if (material.contains(fragment)) return true;
        return false;
    }

    public static boolean toolExempt(String mainHand, String offHand) {
        return mainHand != null && mainHand.contains("COPPER") || offHand != null && offHand.contains("COPPER");
    }

    public static long modernBudgetMillis(double breakSpeed) {
        if (!Double.isFinite(breakSpeed) || breakSpeed <= 0) throw new IllegalArgumentException("A known positive block break speed is required.");
        double result = Math.ceil(1 / breakSpeed) * 50;
        return result >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) result;
    }
}
