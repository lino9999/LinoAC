package io.github.lino9999.linoac.detection.collector;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CinematicState {
    public float rawYaw() { return yaw; }
    public float rawPitch() { return pitch; }
    private float yaw;
    private float pitch;
    private float yawDelta;
    private float pitchDelta;
    private float previousYawDelta;
    private float previousPitchDelta;
    private float signedPitchDelta;
    private double sensitivity;
    private int lastAbruptTick;
    private int lastNegativeSensitivityTick;
    private int lastMatchedTick;
    private boolean settled;
    private boolean negativeSensitivityRecent;
    private boolean cinematic;
    private float pitchQuantum;
    private final int[] sensitivityWindow = new int[40];
    private int pendingSamples;
    private long sensitivitySamples;
    private int sensitivityMode;
    private double normalizedSensitivity;
    private float mouseStep;
    private float yawCount;
    private float pitchCount;
    private boolean modeKnown;
    private float yawSmoothingResidual;
    private float previousYawSmoothingResidual;
    private float pitchSmoothingResidual;
    private float previousPitchSmoothingResidual;

    public Snapshot rotate(float nextYaw, float nextPitch, int serverTick) {
        if (!Float.isFinite(nextYaw) || !Float.isFinite(nextPitch)) throw new IllegalArgumentException("Rotation must be finite.");
        float nextYawDelta = Math.abs(nextYaw - yaw);
        signedPitchDelta = nextPitch - pitch;
        float nextPitchDelta = Math.abs(signedPitchDelta);
        float yawAcceleration = Math.abs(nextYawDelta - yawDelta);
        float pitchAcceleration = Math.abs(nextPitchDelta - pitchDelta);
        float sensitivityFactor = (float) normalizedSensitivity * .6f + .2f;
        mouseStep = sensitivityFactor * sensitivityFactor * sensitivityFactor * 1.2f;
        yawCount = nextYawDelta / mouseStep;
        pitchCount = nextPitchDelta / mouseStep;
        float yawResidual = Math.abs(yawAcceleration - nextYawDelta);
        float pitchResidual = Math.abs(pitchAcceleration - nextPitchDelta);
        if (yawResidual > 1 && pitchResidual > 1) lastAbruptTick = serverTick;
        if (nextPitchDelta < 20 && sensitivity < 0) lastNegativeSensitivityTick = serverTick;
        if (settled && negativeSensitivityRecent) lastMatchedTick = serverTick;
        settled = serverTick - lastAbruptTick > 3;
        negativeSensitivityRecent = serverTick - lastNegativeSensitivityTick < 3;
        cinematic = serverTick - lastMatchedTick < 8;
        float adjustedYaw = nextYawDelta * 1.073742f + (float) (nextYawDelta + .15);
        float adjustedPitch = nextPitchDelta * 1.073742f - (float) (nextPitchDelta - .15);
        previousYawSmoothingResidual = yawSmoothingResidual;
        previousPitchSmoothingResidual = pitchSmoothingResidual;
        yawSmoothingResidual = Math.abs(nextYawDelta - Math.abs(nextYawDelta - adjustedYaw));
        pitchSmoothingResidual = Math.abs(nextPitchDelta - Math.abs(nextPitchDelta - adjustedPitch));
        if (nextPitchDelta > .1 && nextPitchDelta < 25) {
            pitchQuantum = (float) quantum(nextPitchDelta, pitchDelta);
            sensitivity = (1.666 * Math.cbrt(.8333 * pitchQuantum) - .3333) * 200;
            sensitivityWindow[pendingSamples++] = (int) sensitivity;
            sensitivitySamples++;
            if (pendingSamples == sensitivityWindow.length) {
                sensitivityMode = mode(sensitivityWindow);
                modeKnown = true;
                if (sensitivityMode > 0 && sensitivityMode < 200) normalizedSensitivity = SensitivityCalibration.normalized(sensitivityMode);
                pendingSamples = 0;
            }
        }
        yaw = nextYaw;
        pitch = nextPitch;
        previousYawDelta = yawDelta;
        previousPitchDelta = pitchDelta;
        yawDelta = nextYawDelta;
        pitchDelta = nextPitchDelta;
        return snapshot();
    }

    public Snapshot snapshot() {
        return new Snapshot(cinematic, sensitivity, pitchQuantum, yawDelta, pitchDelta, sensitivityMode,
                normalizedSensitivity, modeKnown, pendingSamples, sensitivitySamples, mouseStep, yawCount, pitchCount,
                yawSmoothingResidual, previousYawSmoothingResidual, pitchSmoothingResidual, previousPitchSmoothingResidual,
                previousYawDelta, previousPitchDelta, signedPitchDelta);
    }

    private static int mode(int[] samples) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int sample : samples) counts.merge(sample, 1, Integer::sum);
        int winner = samples[0], maximum = 0;
        for (var count : counts.entrySet()) if (count.getValue() > maximum) {
            winner = count.getKey();
            maximum = count.getValue();
        }
        return winner;
    }

    public static double quantum(double first, double second) {
        if (!Double.isFinite(first) || !Double.isFinite(second) || first < 0 || second < 0) throw new IllegalArgumentException("Rotation deltas must be finite and nonnegative.");
        double larger = Math.max(first, second), smaller = Math.min(first, second);
        while (Math.abs(smaller) >= .001) {
            double remainder = larger - Math.floor(larger / smaller) * smaller;
            larger = smaller;
            smaller = remainder;
        }
        return larger;
    }

    public record Snapshot(boolean cinematic, double sensitivity, float pitchQuantum, float yawDelta, float pitchDelta,
                           int sensitivityMode, double normalizedSensitivity, boolean modeKnown, int pendingSamples,
                           long sensitivitySamples, float mouseStep, float yawCount, float pitchCount,
                           float yawSmoothingResidual, float previousYawSmoothingResidual,
                           float pitchSmoothingResidual, float previousPitchSmoothingResidual,
                           float previousYawDelta, float previousPitchDelta, float signedPitchDelta) {}
}
