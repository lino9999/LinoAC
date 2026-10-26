package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.Observation;
import java.util.HashMap;

public final class ProtocolContext {
    private int groundRun;
    private int airRun;
    private int bucketAge = 100;
    private int inactiveResetPositionAge = 100;
    private long lastFlyingMillis;
    private long lastFastMoveMillis;
    private long flyingIntervalMillis;
    private boolean fastMove;
    private boolean placement;
    private boolean entityAction;
    private boolean digging;
    private boolean arm;

    public ProtocolContext(long createdWallMillis) { lastFastMoveMillis = createdWallMillis; }

    public Observation update(Observation observation) {
        if (observation.state("bukkit-event")) return observation;
        if (observation.kind() == Observation.Kind.MOVE) {
            boolean ground = observation.state("client-ground");
            groundRun = ground ? increment(groundRun) : 0;
            airRun = ground ? 0 : increment(airRun);
            bucketAge = increment(bucketAge);
            if (observation.state("position-packet")) inactiveResetPositionAge = increment(inactiveResetPositionAge);
            placement = entityAction = digging = arm = false;
            if (observation.has("packet-time-ms")) {
                long now = (long) observation.value("packet-time-ms");
                flyingIntervalMillis = now - lastFlyingMillis;
                fastMove = flyingIntervalMillis <= 5;
                if (fastMove) lastFastMoveMillis = now;
                lastFlyingMillis = now;
            }
        } else {
            if (observation.state("packet:PLAYER_BLOCK_PLACEMENT")) placement = true;
            if (observation.state("packet:ENTITY_ACTION")) entityAction = true;
            if (observation.state("packet:PLAYER_DIGGING")) digging = true;
            if (observation.state("packet:ANIMATION") || observation.state("packet:PUNCH")) arm = true;
        }
        var values = new HashMap<>(observation.values());
        values.put("protocol-ground-run", (double) groundRun);
        values.put("source-client-ground-movement-age", (double) groundRun);
        values.put("source-inactive-reset-position-age", (double) inactiveResetPositionAge);
        values.put("protocol-air-run", (double) airRun);
        values.put("source-client-air-movement-age", (double) airRun);
        values.put("protocol-bucket-age", (double) bucketAge);
        values.put("protocol-last-fast-move-ms", (double) lastFastMoveMillis);
        values.put("protocol-fast-move", fastMove ? 1d : 0d);
        values.put("protocol-flying-interval-ms", (double) flyingIntervalMillis);
        values.put("protocol-frame-placement", placement ? 1d : 0d);
        values.put("protocol-frame-entity-action", entityAction ? 1d : 0d);
        values.put("protocol-frame-digging", digging ? 1d : 0d);
        values.put("protocol-frame-arm", arm ? 1d : 0d);
        return new Observation(observation.kind(), observation.tick(), observation.nanos(), values, observation.states());
    }

    public void bucketInteraction() { bucketAge = 0; }

    public Snapshot snapshot() { return new Snapshot(groundRun, airRun, bucketAge, fastMove, lastFastMoveMillis, placement, entityAction, digging, arm, inactiveResetPositionAge); }

    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }

    public record Snapshot(int groundRun, int airRun, int bucketAge, boolean fastMove, long lastFastMoveMillis,
                           boolean placement, boolean entityAction, boolean digging, boolean arm, int inactiveResetPositionAge) {}
}
