package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.Observation;

import java.util.HashMap;
import java.util.HashSet;

public final class VelocityPrediction {
    private static final float[] SINE = new float[65536];
    private static final int[][] DIRECTIONS = {{1, 0}, {1, -1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {0, 0}};
    private static final String[] REQUIRED = {"source-position-x", "source-position-z", "source-previous-position-x", "source-previous-position-z",
            "source-previous-position-dx", "source-previous-position-dy", "source-previous-position-dz", "source-previous-ground", "source-previous-previous-ground",
            "source-yaw", "protocol-movement-speed", "protocol-speed-amplifier", "protocol-jump-amplifier", "source-sneaking",
            "client-newer-than-1-9", "client-at-least-1-13", "source-ceiling-position-age"};
    private boolean lastPosition;
    private boolean previousPosition;
    private int attacks;

    static {
        for (int i = 0; i < SINE.length; i++) SINE[i] = (float) Math.sin(i * Math.PI * 2 / SINE.length);
    }

    public Observation update(Observation input, VelocityContext.Snapshot velocity) {
        var values = new HashMap<>(input.values());
        var states = new HashSet<>(input.states());
        values.remove("source-velocity-d-minimum-error");
        values.remove("source-velocity-d-threshold");
        states.remove("source-velocity-d-known");
        if (input.state("nonfinite-position") || input.state("nonfinite-rotation") || input.state("outside-world") || input.state("invalid-pitch")) {
            return new Observation(input.kind(), input.tick(), input.nanos(), values, states);
        }
        if (!input.state("bukkit-event") && input.kind() == Observation.Kind.ATTACK) {
            if (attacks < Integer.MAX_VALUE) attacks++;
        } else if (input.kind() == Observation.Kind.MOVE && !input.state("source-velocity-d-early-return")) {
            if (velocity.complete() && input.state("source-velocity-d-input-known") && !input.state("source-velocity-d-exempt")
                    && present(input) && velocity.velocityPositionAge() <= 6 && input.value("source-ceiling-position-age") >= 3
                    && (velocity.velocityX() >= .01 || velocity.velocityY() >= .01 || velocity.velocityZ() >= .01)) {
                Input motion = new Input(input.value("source-previous-position-x"), input.value("source-previous-position-z"),
                        input.value("source-position-x"), input.value("source-position-z"), input.value("source-previous-position-dx"),
                        input.value("source-previous-position-dy"), input.value("source-previous-position-dz"), (float) input.value("source-yaw"),
                        input.value("source-previous-ground") != 0, input.value("source-previous-previous-ground") != 0,
                        input.value("source-sneaking") != 0, input.value("client-newer-than-1-9") != 0, input.value("client-at-least-1-13") != 0,
                        input.value("protocol-movement-speed"), (int) input.value("protocol-speed-amplifier"),
                        (int) input.value("protocol-jump-amplifier"), attacks, velocity);
                double error = minimumError(motion);
                if (Double.isFinite(error)) {
                    states.add("source-velocity-d-known");
                    values.put("source-velocity-d-minimum-error", error);
                    values.put("source-velocity-d-threshold", 5E-6 + (lastPosition && previousPosition ? 0 : .001) + (motion.modernDrag ? .001 : 0));
                }
            }
            previousPosition = lastPosition;
            lastPosition = input.state("position-packet");
            attacks = 0;
        }
        return new Observation(input.kind(), input.tick(), input.nanos(), values, states);
    }

    private boolean present(Observation input) {
        for (String key : REQUIRED) if (!input.has(key) || !Double.isFinite(input.value(key))) return false;
        return true;
    }

    public static double minimumError(Input input) {
        if (input == null || input.velocity == null || !input.velocity.complete()
                || !Double.isFinite(input.previousX) || !Double.isFinite(input.previousZ) || !Double.isFinite(input.currentX)
                || !Double.isFinite(input.currentZ) || !Double.isFinite(input.previousDeltaX) || !Double.isFinite(input.previousDeltaY)
                || !Double.isFinite(input.previousDeltaZ) || !Float.isFinite(input.yaw) || !Double.isFinite(input.movementSpeed)) return Double.NaN;
        double minimum = Double.MAX_VALUE;
        float friction = input.previousGround ? .54600006f : .91f;
        float priorFriction = input.previousPreviousGround ? .54600006f : .91f;
        for (int[] direction : DIRECTIONS) {
            for (int options = 0; options < 16; options++) {
                boolean sprint = (options & 1) != 0;
                boolean using = (options & 2) != 0;
                boolean slowingAttack = (options & 4) != 0;
                boolean jump = (options & 8) != 0;
                float forward = direction[0];
                float strafe = direction[1];
                if (using) { forward = (float) (forward * .2); strafe = (float) (strafe * .2); }
                if (input.sneaking) { forward *= .3f; strafe *= .3f; }
                forward *= .98f;
                strafe *= .98f;
                double x = input.velocity.velocityPending() ? input.velocity.velocityX() : input.previousDeltaX;
                double y = input.velocity.velocityPending() ? input.velocity.velocityY() : input.previousDeltaY;
                double z = input.velocity.velocityPending() ? input.velocity.velocityZ() : input.previousDeltaZ;
                double speed = (float) (input.movementSpeed + input.speedAmplifier * .2 * input.movementSpeed);
                if (sprint) speed += speed * .30000001192092896;
                float coefficient = (input.modernMovement ? .21600002f : .16277136f) / (friction * friction * friction);
                float acceleration = input.previousGround ? (float) speed * coefficient : (float) (sprint ? .025999999418854714 : .019999999552965164);
                if (!input.modernDrag && input.attacks > 0 && slowingAttack) { x *= .6; z *= .6; }
                if (!input.velocity.velocityPending()) { y = (y - .08) * .9800000190734863; x *= priorFriction; z *= priorFriction; }
                double deadzone = input.modernDrag ? .003 : .005;
                if (Math.abs(x) < deadzone) x = 0;
                if (Math.abs(y) < deadzone) y = 0;
                if (Math.abs(z) < deadzone) z = 0;
                if (jump && input.previousGround) {
                    y = .41999998688697815;
                    if (input.jumpAmplifier > 0) y += input.jumpAmplifier * .1f;
                    if (sprint) { float radians = input.yaw * .017453292f; x -= sin(radians) * .2f; z += cos(radians) * .2f; }
                }
                float squared = strafe * strafe + forward * forward;
                if (squared > 1E-4f) {
                    float length = (float) Math.sqrt(squared);
                    if (length < 1) length = 1;
                    float scale = acceleration / length;
                    float scaledStrafe = strafe * scale;
                    float scaledForward = forward * scale;
                    float radians = input.yaw * 3.1415927f / 180f;
                    float sine = sin(radians), cosine = cos(radians);
                    x += scaledStrafe * cosine - scaledForward * sine;
                    z += scaledForward * cosine + scaledStrafe * sine;
                }
                double dx = input.currentX - (input.previousX + x);
                double dz = input.currentZ - (input.previousZ + z);
                double error = dx * dx + dz * dz;
                if (error < minimum) minimum = error;
            }
        }
        return minimum;
    }

    private static float sin(float radians) { return SINE[(int) (radians * 10430.378f) & 65535]; }
    private static float cos(float radians) { return SINE[(int) (radians * 10430.378f + 16384f) & 65535]; }

    public record Input(double previousX, double previousZ, double currentX, double currentZ,
                        double previousDeltaX, double previousDeltaY, double previousDeltaZ, float yaw,
                        boolean previousGround, boolean previousPreviousGround, boolean sneaking,
                        boolean modernDrag, boolean modernMovement, double movementSpeed, int speedAmplifier,
                        int jumpAmplifier, int attacks, VelocityContext.Snapshot velocity) {}
}
