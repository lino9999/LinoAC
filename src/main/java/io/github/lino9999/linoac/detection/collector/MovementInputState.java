package io.github.lino9999.linoac.detection.collector;

public final class MovementInputState {
    private double gravityRevision, jumpRevision;
    private int gravityAge, jumpAge, clientAir, positions;
    private boolean gravityKnown = true, jumpKnown = true;
    private int fireworkAge = 200;
    private boolean positionPacket, previousPositionPacket, previousPreviousPositionPacket;
    private int alignedPositions, physicalGlidingPositions, physicalGlidingAge;
    private boolean physicalGliding;
    private int sprintPositions, walkPositions;
    private final ContactAgeEvidence sulfur = new ContactAgeEvidence(1000, 100);
    private Frame current = new Frame(0, false, false), previous = current;
    private final BlockAnchor placement = new BlockAnchor(12, false);
    private final BlockAnchor broken = new BlockAnchor(8, true);

    public void placement(double x, double y, double z) {
        placement.select(x, y, z);
    }

    public void updatePlacementDistance(double x, double y, double z) {
        placement.position(x, y, z);
        broken.position(x, y, z);
    }

    public double placementDistance() { return placement.distance(); }
    public void broken(double x, double y, double z) { broken.select(x, y, z); }
    public double breakDistance() { return broken.distance(); }
    public void sulfur(Boolean contact) { sulfur.position(contact); }
    public Integer sulfurAge() { return sulfur.age(); }
    public void firework(boolean nearby, boolean held) { fireworkAge = held ? 0 : nearby ? 1 : increment(fireworkAge); }
    public int fireworkAge() { return fireworkAge; }
    public void packet(boolean position) { previousPreviousPositionPacket = previousPositionPacket; previousPositionPacket = positionPacket; positionPacket = position; }
    public boolean previousPositionPacket() { return previousPositionPacket; }
    public boolean previousPreviousPositionPacket() { return previousPreviousPositionPacket; }
    public void worldPosition(double y, boolean gliding) {
        alignedPositions = y % .015625 == 0 ? increment(alignedPositions) : 0;
        physicalGlidingPositions = gliding ? increment(physicalGlidingPositions) : 0;
    }
    public void equipmentPosition(boolean elytra, boolean gliding) {
        physicalGliding = elytra && gliding;
        physicalGlidingAge = physicalGliding ? 0 : increment(physicalGlidingAge);
    }
    public void physicalValues(java.util.Map<String, Double> values) {
        values.put("source-y-aligned-position-count", (double) alignedPositions);
        values.put("source-physical-gliding-position-count", (double) physicalGlidingPositions);
        values.put("source-physical-gliding", physicalGliding ? 1d : 0d);
        values.put("source-physical-gliding-position-age", (double) physicalGlidingAge);
        values.put("source-sprint-position-count", (double) sprintPositions);
        values.put("source-walk-position-count", (double) walkPositions);
    }
    public void sprintPosition(boolean sprinting) {
        sprintPositions = sprinting ? increment(sprintPositions) : 0;
        walkPositions = sprinting ? 0 : increment(walkPositions);
    }
    public boolean physicalGliding() { return physicalGliding; }

    public void acknowledge(Double gravity, Double jump) {
        gravityKnown = gravity != null && Double.isFinite(gravity);
        jumpKnown = jump != null && Double.isFinite(jump);
        if (gravityKnown && gravity != gravityRevision) { gravityRevision = gravity; gravityAge = 0; }
        if (jumpKnown && jump != jumpRevision) { jumpRevision = jump; jumpAge = 0; }
    }

    public void flying() {
        if (gravityKnown) gravityAge = increment(gravityAge);
        if (jumpKnown) jumpAge = increment(jumpAge);
    }

    public void position(boolean clientGround) {
        position(clientGround, 0, false);
    }

    public void position(boolean clientGround, double dy, boolean matched) {
        if (!Double.isFinite(dy)) throw new IllegalArgumentException("Position delta must be finite.");
        positions = increment(positions);
        clientAir = clientGround ? 0 : increment(clientAir);
        previous = current;
        current = new Frame(dy, clientGround, matched);
    }

    public Snapshot snapshot() { return new Snapshot(gravityKnown, gravityAge, jumpKnown, jumpAge, clientAir, positions, current, previous); }
    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
    public record Frame(double dy, boolean ground, boolean matched) {}
    public record Snapshot(boolean gravityKnown, int gravityAge, boolean jumpKnown, int jumpAge, int clientAir, int positions, Frame current, Frame previous) {}
}
