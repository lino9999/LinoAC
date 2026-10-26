package io.github.lino9999.linoac.detection.collector;

public final class BlockAnchor {
    private final double maximumDistance;
    private final boolean clearDistanceWhenExpired;
    private double x, y, z, distance;
    private boolean known;

    public BlockAnchor(double maximumDistance, boolean clearDistanceWhenExpired) {
        if (!Double.isFinite(maximumDistance) || maximumDistance <= 0) throw new IllegalArgumentException("Anchor distance must be positive.");
        this.maximumDistance = maximumDistance; this.clearDistanceWhenExpired = clearDistanceWhenExpired;
    }

    public void select(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        this.x = x; this.y = y; this.z = z; known = true;
    }

    public void position(double x, double y, double z) {
        if (!known || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        double dx = x - this.x, dy = y - this.y, dz = z - this.z;
        distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > maximumDistance) { known = false; if (clearDistanceWhenExpired) distance = 0; }
    }

    public double distance() { return distance; }
    public boolean active() { return known; }
}
