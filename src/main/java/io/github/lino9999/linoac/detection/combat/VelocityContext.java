package io.github.lino9999.linoac.detection.combat;

import java.util.ArrayDeque;

public final class VelocityContext {
    private static final int CAPACITY = 512;
    private final ArrayDeque<Motion> velocities = new ArrayDeque<>();
    private final ArrayDeque<Motion> explosions = new ArrayDeque<>();
    private boolean complete = true;
    private boolean velocityPending;
    private boolean explosionPending;
    private int velocityPolls;
    private int explosionPolls;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private double acknowledgedVelocityY;
    private double maximumHorizontal;
    private double maximumVertical;
    private double explosionX;
    private double explosionY;
    private double explosionZ;
    private double explosionSumY;
    private double explosionSumHorizontal;
    private double lastPositiveExplosionY;
    private double lastPositiveExplosionHorizontal;
    private int velocityPositionAge;
    private int explosionPositionAge;
    private int verticalPositionAge;

    public void invalidate() { complete = false; }

    public void beginVelocity(double x, double y, double z) {
        if (!valid(x, y, z) || velocities.size() == CAPACITY) { complete = false; return; }
        acknowledgedVelocityY = y;
        verticalPositionAge = 0;
        velocities.addLast(new Motion(x, y, z));
    }

    public void endVelocity() {
        if (velocityPolls == CAPACITY) { complete = false; return; }
        velocityPolls++;
    }

    public void beginExplosion(double x, double y, double z) {
        if (!valid(x, y, z) || explosions.size() == CAPACITY) { complete = false; return; }
        if (x != 0 || y != 0 || z != 0) explosions.addLast(new Motion(x, y, z));
    }

    public void endExplosion() {
        if (explosionPolls == CAPACITY) { complete = false; return; }
        explosionPolls++;
    }

    public Snapshot position(boolean clientGround) {
        velocityPending = !velocities.isEmpty();
        explosionPending = !explosions.isEmpty();
        for (Motion motion : velocities) {
            velocityX = motion.x;
            velocityY = motion.y;
            velocityZ = motion.z;
            maximumHorizontal = Math.max(maximumHorizontal, Math.sqrt(motion.x * motion.x + motion.z * motion.z));
            maximumVertical = Math.max(maximumVertical, motion.y);
        }
        explosionSumY = 0;
        explosionSumHorizontal = 0;
        for (Motion motion : explosions) {
            explosionX = motion.x;
            explosionY = motion.y;
            explosionZ = motion.z;
            explosionSumY += motion.y;
            explosionSumHorizontal += Math.hypot(motion.x, motion.z);
        }
        if (!Double.isFinite(explosionSumY) || !Double.isFinite(explosionSumHorizontal)) complete = false;
        if (explosionSumY > 0) lastPositiveExplosionY = explosionSumY;
        if (explosionSumHorizontal > 0) lastPositiveExplosionHorizontal = explosionSumHorizontal;
        if (clientGround) lastPositiveExplosionY = 0;
        velocityPositionAge = velocityPending ? 0 : increment(velocityPositionAge);
        explosionPositionAge = explosionPending ? 0 : increment(explosionPositionAge);
        verticalPositionAge = increment(verticalPositionAge);
        return snapshot();
    }

    public void afterPosition() {
        for (int i = 0; i < velocityPolls; i++) velocities.pollFirst();
        for (int i = 0; i < explosionPolls; i++) explosions.pollFirst();
        velocityPolls = 0;
        explosionPolls = 0;
    }

    public Snapshot snapshot() {
        return new Snapshot(complete, velocityPending, explosionPending, velocities.size(), explosions.size(),
                velocityX, velocityY, velocityZ, acknowledgedVelocityY, maximumHorizontal, maximumVertical,
                explosionX, explosionY, explosionZ, explosionSumY, explosionSumHorizontal, lastPositiveExplosionY,
                lastPositiveExplosionHorizontal, velocityPositionAge, explosionPositionAge, verticalPositionAge);
    }

    private boolean valid(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && Double.isFinite(Math.hypot(x, z))
                && Double.isFinite(x * x + z * z);
    }

    private int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }

    private record Motion(double x, double y, double z) {}

    public record Snapshot(boolean complete, boolean velocityPending, boolean explosionPending, int pendingVelocities,
                           int pendingExplosions, double velocityX, double velocityY, double velocityZ,
                           double acknowledgedVelocityY, double maximumHorizontal, double maximumVertical,
                           double explosionX, double explosionY, double explosionZ, double explosionSumY,
                           double explosionSumHorizontal, double lastPositiveExplosionY, double lastPositiveExplosionHorizontal,
                           int velocityPositionAge, int explosionPositionAge, int verticalPositionAge) {}
}
