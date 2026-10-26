package io.github.lino9999.linoac.detection.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HorizontalVelocityModel {
    private final List<Input> inputs = new ArrayList<>();
    private double impulseX, impulseY, impulseZ;
    private long sentNanos;
    private int acknowledgementIndex = -1;
    private boolean valid;

    public void outgoing(double x, double y, double z, long nanos) {
        inputs.clear();
        impulseX = x;
        impulseY = y;
        impulseZ = z;
        sentNanos = nanos;
        acknowledgementIndex = -1;
        valid = Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && nanos > 0;
    }

    public void acknowledge(long acknowledgementSentNanos) {
        if (valid && acknowledgementSentNanos > sentNanos && acknowledgementIndex < 0) acknowledgementIndex = inputs.size();
    }

    public void invalidate() { valid = false; inputs.clear(); }

    public Optional<Bounds> movement(Input input) {
        if (!valid) return Optional.empty();
        if (inputs.size() >= 32 || !finite(input)) {
            invalidate();
            return Optional.empty();
        }
        inputs.add(input);
        if (acknowledgementIndex < 0) return Optional.empty();
        double minimum = Double.POSITIVE_INFINITY, maximum = 0;
        double verticalMinimum = Double.POSITIVE_INFINITY, verticalMaximum = Double.NEGATIVE_INFINITY;
        double projectionMinimum = Double.POSITIVE_INFINITY, projectionMaximum = Double.NEGATIVE_INFINITY;
        double impulseLength = Math.hypot(impulseX, impulseZ);
        double unitX = impulseLength == 0 ? 0 : impulseX / impulseLength, unitZ = impulseLength == 0 ? 0 : impulseZ / impulseLength;
        for (int start = 0; start <= Math.min(acknowledgementIndex, inputs.size() - 1); start++) {
            double xMin = impulseX, xMax = impulseX, zMin = impulseZ, zMax = impulseZ;
            double yMin = impulseY, yMax = impulseY;
            double lastMin = 0, lastMax = 0, lastYMin = 0, lastYMax = 0, lastProjectionMin = 0, lastProjectionMax = 0;
            for (int i = start; i < inputs.size(); i++) {
                Input sample = inputs.get(i);
                if (sample.attacked()) {
                    xMin = Math.min(xMin, xMin * .6); xMax = Math.max(xMax, xMax * .6);
                    zMin = Math.min(zMin, zMin * .6); zMax = Math.max(zMax, zMax * .6);
                }
                double acceleration = sample.ground()
                        ? sample.movementSpeed() * .21600002 / Math.pow(sample.blockFriction(), 3)
                        : sample.sprinting() ? .026 : .02;
                double[] accelerationRange = inputRange(sample, acceleration);
                xMin += accelerationRange[0]; xMax += accelerationRange[1];
                zMin += accelerationRange[2]; zMax += accelerationRange[3];
                lastMin = Math.hypot(axisDistance(xMin, xMax), axisDistance(zMin, zMax));
                lastMax = Math.hypot(Math.max(Math.abs(xMin), Math.abs(xMax)), Math.max(Math.abs(zMin), Math.abs(zMax)));
                lastProjectionMin = (unitX >= 0 ? xMin : xMax) * unitX + (unitZ >= 0 ? zMin : zMax) * unitZ;
                lastProjectionMax = (unitX >= 0 ? xMax : xMin) * unitX + (unitZ >= 0 ? zMax : zMin) * unitZ;
                double minimumFriction = sample.ground() ? sample.blockFriction() * .91 : .91;
                xMin = Math.min(xMin * minimumFriction, xMin * .91); xMax = Math.max(xMax * minimumFriction, xMax * .91);
                zMin = Math.min(zMin * minimumFriction, zMin * .91); zMax = Math.max(zMax * minimumFriction, zMax * .91);
                if ((!sample.known() || sample.jumping()) && sample.ground()) { yMin = Math.min(yMin, sample.jumpStrength()); yMax = Math.max(yMax, sample.jumpStrength()); }
                if (sample.ground() && yMin < 0) yMax = Math.max(0, yMax);
                lastYMin = yMin; lastYMax = yMax;
                yMin = (yMin - sample.gravity()) * .98; yMax = (yMax - sample.gravity()) * .98;
            }
            minimum = Math.min(minimum, lastMin); maximum = Math.max(maximum, lastMax);
            verticalMinimum = Math.min(verticalMinimum, lastYMin); verticalMaximum = Math.max(verticalMaximum, lastYMax);
            projectionMinimum = Math.min(projectionMinimum, lastProjectionMin); projectionMaximum = Math.max(projectionMaximum, lastProjectionMax);
        }
        return Double.isFinite(minimum) ? Optional.of(new Bounds(Math.max(0, minimum - .025), maximum + .025, verticalMinimum - .005, verticalMaximum + .005,
                projectionMinimum - .025, projectionMaximum + .025)) : Optional.empty();
    }

    private double[] inputRange(Input sample, double acceleration) {
        double xMin = 0, xMax = 0, zMin = 0, zMax = 0;
        for (int side = -1; side <= 1; side++) for (int forward = -1; forward <= 1; forward++) {
            if (sample.known() && (side != 0 || forward != 0)) continue;
            double sideways = (sample.known() ? sample.sideways() : side) * .98;
            double straight = (sample.known() ? sample.forward() : forward) * .98;
            double length = Math.hypot(sideways, straight);
            if (length > 1) { sideways /= length; straight /= length; }
            double radians = Math.toRadians(sample.yaw());
            double x = (sideways * Math.cos(radians) - straight * Math.sin(radians)) * acceleration * 1.3;
            double z = (straight * Math.cos(radians) + sideways * Math.sin(radians)) * acceleration * 1.3;
            xMin = Math.min(xMin, x); xMax = Math.max(xMax, x);
            zMin = Math.min(zMin, z); zMax = Math.max(zMax, z);
        }
        if (sample.ground() && (!sample.known() || sample.jumping()) && (!sample.known() || sample.sprinting())) {
            double radians = Math.toRadians(sample.yaw());
            double x = -.2 * Math.sin(radians), z = .2 * Math.cos(radians);
            xMin += Math.min(0, x); xMax += Math.max(0, x);
            zMin += Math.min(0, z); zMax += Math.max(0, z);
        }
        return new double[]{xMin, xMax, zMin, zMax};
    }

    private double axisDistance(double minimum, double maximum) { return minimum > 0 ? minimum : maximum < 0 ? -maximum : 0; }

    private boolean finite(Input input) {
        return Double.isFinite(input.forward()) && Double.isFinite(input.sideways()) && Math.abs(input.forward()) <= 1
                && Math.abs(input.sideways()) <= 1 && Double.isFinite(input.yaw()) && Double.isFinite(input.movementSpeed())
                && input.movementSpeed() > 0 && input.movementSpeed() <= 10 && Double.isFinite(input.blockFriction())
                && input.blockFriction() >= .1 && input.blockFriction() <= 1 && Double.isFinite(input.gravity())
                && input.gravity() >= 0 && input.gravity() <= 10 && Double.isFinite(input.jumpStrength()) && input.jumpStrength() >= 0;
    }

    public record Input(boolean known, double forward, double sideways, double yaw, double movementSpeed,
                        double blockFriction, double gravity, double jumpStrength, boolean ground, boolean sprinting, boolean jumping, boolean attacked) {}
    public record Bounds(double minimum, double maximum, double verticalMinimum, double verticalMaximum, double projectionMinimum, double projectionMaximum) {}
}
