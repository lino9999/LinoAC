package io.github.lino9999.linoac.packet;

import java.util.HashMap;
import java.util.Map;

public final class EntityPositionTracker {
    private final Map<Integer, Position> positions = new HashMap<>();
    private final int capacity;
    private boolean known = true;

    public EntityPositionTracker(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Entity capacity must be positive.");
        this.capacity = capacity;
    }

    public void apply(Update update) {
        if (update.kind() == UpdateKind.DESTROY) { positions.remove(update.id()); return; }
        if (!Double.isFinite(update.x()) || !Double.isFinite(update.y()) || !Double.isFinite(update.z())) { invalidate(); return; }
        Position previous = positions.get(update.id());
        if (update.kind() != UpdateKind.SPAWN && previous == null) return;
        if (previous == null && positions.size() >= capacity) { invalidate(); return; }
        String type = update.kind() == UpdateKind.SPAWN ? update.type() : previous.type();
        Boolean living = update.kind() == UpdateKind.SPAWN ? update.living() : previous.living();
        Position next = update.kind() == UpdateKind.RELATIVE ? new Position(previous.x() + update.x(), previous.y() + update.y(), previous.z() + update.z(), type, living)
                : new Position(update.x(), update.y(), update.z(), type, living);
        if (!Double.isFinite(next.x()) || !Double.isFinite(next.y()) || !Double.isFinite(next.z())) { invalidate(); return; }
        positions.put(update.id(), next);
    }

    public int nearby(double x, double y, double z) {
        if (!known || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return -1;
        int count = 0;
        for (Position position : positions.values()) {
            double dx = position.x() - x, dy = position.y() - y, dz = position.z() - z;
            if (dx * dx + dy * dy + dz * dz <= 4) count++;
        }
        return count;
    }

    public Map<Integer, Position> snapshot() { return Map.copyOf(positions); }
    public int nearbyLiving(double x, double y, double z) {
        if (!known || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return -1;
        int count = 0; boolean unknown = false;
        for (Position position : positions.values()) {
            double dx = position.x() - x, dy = position.y() - y, dz = position.z() - z;
            if (dx * dx + dy * dy + dz * dz <= 4) {
                if (position.living() == null) unknown = true;
                else if (position.living()) count++;
            }
        }
        return count > 0 ? count : unknown ? -1 : 0;
    }
    public int nearbyBoats(double x, double y, double z) {
        if (!known || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return -1;
        int count = 0;
        for (Position position : positions.values()) {
            double dx = position.x() - x, dy = position.y() - y, dz = position.z() - z;
            if (dx * dx + dy * dy + dz * dz <= 4 && (position.type().contains("BOAT") || position.type().contains("RAFT"))) count++;
        }
        return count;
    }
    public boolean known() { return known; }
    public void invalidate() { positions.clear(); known = false; }
    public void reset() { positions.clear(); known = true; }

    public record Position(double x, double y, double z, String type, Boolean living) {
        public Position(double x, double y, double z) { this(x, y, z, "UNKNOWN", null); }
        public Position(double x, double y, double z, String type) { this(x, y, z, type, null); }
    }
    public enum UpdateKind { SPAWN, ABSOLUTE, RELATIVE, DESTROY }
    public record Update(int id, double x, double y, double z, UpdateKind kind, String type, Boolean living) {
        public Update(int id, double x, double y, double z, UpdateKind kind) { this(id, x, y, z, kind, "UNKNOWN", null); }
        public Update(int id, double x, double y, double z, UpdateKind kind, String type) { this(id, x, y, z, kind, type, null); }
        public Update {
            java.util.Objects.requireNonNull(kind);
            type = type == null ? "UNKNOWN" : type.toUpperCase(java.util.Locale.ROOT);
        }
        public static Update spawn(int id, double x, double y, double z) { return spawn(id, x, y, z, "UNKNOWN"); }
        public static Update spawn(int id, double x, double y, double z, String type) { return new Update(id, x, y, z, UpdateKind.SPAWN, type); }
        public static Update spawn(int id, double x, double y, double z, String type, boolean living) { return new Update(id, x, y, z, UpdateKind.SPAWN, type, living); }
        public static Update move(int id, double x, double y, double z, boolean relative) { return new Update(id, x, y, z, relative ? UpdateKind.RELATIVE : UpdateKind.ABSOLUTE); }
        public static Update destroy(int id) { return new Update(id, 0, 0, 0, UpdateKind.DESTROY); }
    }
}
