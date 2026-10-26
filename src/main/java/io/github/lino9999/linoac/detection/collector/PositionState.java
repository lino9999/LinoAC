package io.github.lino9999.linoac.detection.collector;

import io.github.lino9999.linoac.packet.ServerWorldSnapshot;

public final class PositionState {
    private final EntityCramState cram = new EntityCramState();
    private double rawX;
    private double rawY;
    private double rawZ;
    private double floorX;
    private double floorY;
    private double floorZ;
    private double signedDy;
    private double dx, dz, previousDx, previousDy, previousDz;
    private double previousX, previousZ;
    private boolean positionKnown;
    private boolean originSkip;
    private boolean webKnown = true;
    private boolean hadKnownWorld;
    private boolean cobweb;
    private int webPositionAge = 100;
    private int vehiclePositionAge = 100;
    private int flightPositionAge = 100;
    private boolean flightKnown;

    public void position(double x, double y, double z, ServerWorldSnapshot world, boolean mounted, int maximumEntities) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || world == null || maximumEntities < 1) throw new IllegalArgumentException("Invalid position state input.");
        double nextDy = y - rawY;
        if (!Double.isFinite(nextDy)) throw new IllegalArgumentException("Invalid position delta.");
        originSkip = floorX == 0 && floorY == 0 && floorZ == 0;
        previousDx = dx; previousDy = signedDy; previousDz = dz;
        previousX = positionKnown ? rawX : x; previousZ = positionKnown ? rawZ : z;
        dx = x - rawX; dz = z - rawZ;
        rawX = x;
        rawY = y;
        rawZ = z;
        signedDy = nextDy;
        positionKnown = true;
        if (world.complete()) {
            floorX = Math.floor(x);
            floorY = Math.floor(y);
            floorZ = Math.floor(z);
            cobweb = world.cobweb();
            hadKnownWorld = true;
            webKnown = true;
        } else if (hadKnownWorld) webKnown = false;
        webPositionAge = cobweb ? 0 : increment(webPositionAge);
        vehiclePositionAge = mounted ? 0 : increment(vehiclePositionAge);
        cram.move(-1, world.nearbyEntities(), maximumEntities);
    }

    public Snapshot snapshot() {
        return new Snapshot(rawX, rawY, rawZ, positionKnown, originSkip, signedDy, webKnown,
                webPositionAge < 25 && signedDy < .1, webPositionAge, vehiclePositionAge, cram.movementAge(),
                dx, dz, previousDx, previousDy, previousDz, flightKnown, flightPositionAge, previousX, previousZ);
    }

    public void flight(boolean known, boolean allowed) {
        flightKnown = known;
        if (known) flightPositionAge = allowed ? 0 : increment(flightPositionAge);
    }

    public boolean cramExempt(boolean enabled, int positions) {
        if (positions < 0) throw new IllegalArgumentException("Position age must be nonnegative.");
        return cram.exempt(enabled, positions);
    }

    private static int increment(int age) { return age == Integer.MAX_VALUE ? age : age + 1; }

    public record Snapshot(double rawX, double rawY, double rawZ, boolean positionKnown, boolean originSkip,
                           double signedDy, boolean webKnown, boolean webExempt, int webPositionAge,
                           int vehiclePositionAge, int cramPositionAge, double dx, double dz,
                           double previousDx, double previousDy, double previousDz, boolean flightKnown, int flightPositionAge, double previousX, double previousZ) {}
}
