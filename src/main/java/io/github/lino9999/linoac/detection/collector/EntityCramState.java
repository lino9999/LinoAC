package io.github.lino9999.linoac.detection.collector;

public final class EntityCramState {
    private int movementAge = 100;

    public void move(int mirroredNearby, int serverNearby, int maximumEntities) {
        if (mirroredNearby < -1 || serverNearby < -1 || maximumEntities < 1) throw new IllegalArgumentException("Invalid entity count.");
        if (mirroredNearby > maximumEntities || serverNearby > maximumEntities) movementAge = 0;
        if (movementAge < Integer.MAX_VALUE) movementAge++;
    }

    public boolean exempt(boolean enabled, int movementPackets) { return enabled && movementAge < movementPackets; }
    public int movementAge() { return movementAge; }
}
