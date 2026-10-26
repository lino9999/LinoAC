package io.github.lino9999.linoac.detection.collector;

import java.util.List;

public final class AttributeMath {
    private AttributeMath() {}

    public static double evaluate(double base, List<Adjustment> adjustments) {
        double added = base;
        for (Adjustment adjustment : adjustments) if (adjustment.operation() == Operation.ADD) added += adjustment.amount();
        double value = added;
        for (Adjustment adjustment : adjustments) if (adjustment.operation() == Operation.SCALE_BASE) value += added * adjustment.amount();
        for (Adjustment adjustment : adjustments) if (adjustment.operation() == Operation.SCALE_TOTAL) value *= 1 + adjustment.amount();
        return value;
    }

    public static double airDrag(double base, double modifier, boolean supportsModifier) {
        return supportsModifier ? Math.max(0, Math.min(1, 1 - (1 - base) * modifier)) : base;
    }

    public enum Operation { ADD, SCALE_BASE, SCALE_TOTAL }
    public record Adjustment(Operation operation, double amount) {}
}
