package io.github.lino9999.linoac.detection.combat;

import java.util.ArrayDeque;

public final class TargetHistory {
    private final ArrayDeque<Entry> entries = new ArrayDeque<>(20);
    private int target = -1;
    private double cachedDistance;

    public void selectPlayer(int entityId) {
        if (entityId < 0) throw new IllegalArgumentException("Invalid target entity ID.");
        if (entityId != target) entries.clear();
        target = entityId;
    }

    public void relative(int entityId, double rawX, double rawZ, double deltaX, double deltaZ, long tick) {
        if (entityId != target || !Double.isFinite(rawX) || !Double.isFinite(rawZ) || !Double.isFinite(deltaX) || !Double.isFinite(deltaZ)) return;
        double x = rawX - deltaX, z = rawZ - deltaZ;
        if (!Double.isFinite(x) || !Double.isFinite(z)) return;
        if (entries.size() == 20) entries.removeFirst();
        entries.addLast(new Entry(x, z, tick));
    }

    public void flying(double x, double z, long tick, int lagTicks) {
        if (!entries.isEmpty() && target >= 0) cachedDistance = distance(x, z, tick, lagTicks);
    }

    public double distance(double x, double z, long tick, int lagTicks) {
        double result = Double.POSITIVE_INFINITY;
        for (Entry entry : entries) if (matching(entry, tick, lagTicks)) { double dx = x - entry.x, dz = z - entry.z; result = Math.min(result, Math.sqrt(dx * dx + dz * dz) - .52); }
        return Double.isInfinite(result) ? -1 : result;
    }

    public double angle(double x, double z, double lookX, double lookZ, long tick, int lagTicks) {
        double result = Double.POSITIVE_INFINITY;
        double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
        for (Entry entry : entries) if (matching(entry, tick, lagTicks)) {
            double dx = entry.x - x, dz = entry.z - z;
            double length = Math.sqrt(dx * dx + dz * dz) * lookLength;
            double cosine = Math.max(-1, Math.min(1, (dx * lookX + dz * lookZ) / length));
            result = Math.min(result, (float) Math.acos(cosine));
        }
        return Double.isInfinite(result) ? -1 : result;
    }

    private boolean matching(Entry entry, long tick, int lagTicks) { return Math.abs(tick - entry.tick - lagTicks) < 3; }
    public int target() { return target; }
    public int size() { return entries.size(); }
    public double cachedDistance() { return cachedDistance; }
    public void serverPosition() { entries.clear(); }
    private record Entry(double x, double z, long tick) {}
}
