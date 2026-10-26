package io.github.lino9999.linoac.detection.world;

public final class WorldPatternMath {
    private WorldPatternMath() {}

    public static long pitchPattern(float current, float previous) {
        long first = (long) (current * 16777216d), second = (long) (previous * 16777216d);
        while (second > 16384) { long remainder = first % second; first = second; second = remainder; }
        return first;
    }

    public static float directionAngle(double dx, double dz, float yaw) {
        double radians = (double) (yaw * 3.1415927f / 180f);
        double x = -Math.sin(radians) * .5, z = Math.cos(radians) * .5;
        double cosine = (dx * x + dz * z) / (Math.sqrt(dx * dx + dz * dz) * Math.sqrt(x * x + z * z));
        return (float) Math.acos(Math.min(1, Math.max(-1, cosine)));
    }

    public static int facing(float yaw) {
        float angle = (yaw - 90f) % 360f;
        if (angle < 0) angle = (float) ((double) angle + 360d);
        if (angle >= 0 && angle < 45 || angle >= 315 && angle < 360) return 4;
        if (angle >= 45 && angle < 135) return 2;
        if (angle >= 135 && angle < 225) return 5;
        if (angle >= 225 && angle < 315) return 3;
        return 6;
    }

    public static float sneakingThreshold(float speedLevel, float walkSpeed) {
        float base = .27f;
        float candidate = base + speedLevel * .0675f + (walkSpeed - .2f) * 3.5f;
        return candidate < base ? base + speedLevel * .0675f : base;
    }
}
