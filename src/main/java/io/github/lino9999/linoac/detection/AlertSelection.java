package io.github.lino9999.linoac.detection;

public final class AlertSelection {
    private AlertSelection() {}

    public static boolean eligible(int violations, int minimum, int interval) {
        if (violations < 0 || minimum < 0 || interval < 1) throw new IllegalArgumentException("Invalid alert selection.");
        return violations > 0 && violations >= minimum && violations % interval == 0;
    }
}
