package io.github.lino9999.linoac.detection.collector;

import java.util.ArrayDeque;

public final class TeleportState {
    private final ArrayDeque<Position> positions = new ArrayDeque<>(15);
    private int movementAge = 100;
    private boolean matched;

    public void acknowledgePosition(double x, double y, double z) {
        finite(x, y, z);
        if (positions.size() == 15) positions.removeFirst();
        positions.addLast(new Position(x, y, z));
    }

    public Snapshot move(boolean positionChanged, boolean rotationChanged, boolean onGround, double x, double y, double z) {
        if (positionChanged) finite(x, y, z);
        matched = false;
        if (positionChanged && rotationChanged && !onGround) {
            var pending = positions.iterator();
            while (pending.hasNext()) {
                Position position = pending.next();
                if (Math.abs(position.x - x) < 1e-8 && Math.abs(position.y - y) < 1e-8 && Math.abs(position.z - z) < 1e-8) {
                    matched = true;
                    pending.remove();
                    break;
                }
            }
        }
        if (matched) movementAge = 0;
        if (movementAge < Integer.MAX_VALUE) movementAge++;
        if (movementAge > 100) positions.clear();
        return snapshot();
    }

    public Snapshot snapshot() {
        return new Snapshot(movementAge, matched, positions.size());
    }

    public void reset() { positions.clear(); movementAge = 100; matched = false; }

    private static void finite(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Teleport position must be finite.");
    }

    private record Position(double x, double y, double z) {}

    public record Snapshot(int movementAge, boolean matched, int pendingPositions) {}
}
