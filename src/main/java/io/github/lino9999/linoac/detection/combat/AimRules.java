package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import io.github.lino9999.linoac.detection.Statistics;

import java.util.Set;

final class AimRules implements RuleModule {
    private static final Set<String> IDS = Set.of("aim-a", "aim-b", "aim-c", "aim-d", "aim-e", "aim-f", "aim-g", "aim-h", "aim-i", "aim-k", "aim-l", "aim-n", "aim-o", "aim-p", "aim-q", "aim-s", "aim-u", "aim-w", "aim-x", "aim-y");

    public Set<String> checkIds() { return IDS; }

    public void evaluate(Observation observation, DetectionMemory memory, FindingSink output) {
        if (observation.kind() != Observation.Kind.ROTATION
                || !observation.hasAll("original-delta-yaw", "original-delta-pitch", "pitch", "attack-movement-age")
                || observation.value("attack-movement-age") >= 3) return;
        FindingSink sink = new FindingSink() {
            public void sample(String id, boolean suspicious, String detail) {
                if (eligible(id, observation)) output.sample(id, suspicious, detail);
            }
            public void sampleAbove(String id, boolean suspicious, String detail) {
                if (eligible(id, observation)) output.sampleAbove(id, suspicious, detail);
            }
            public void direct(String id, String detail) {
                if (eligible(id, observation)) output.direct(id, detail);
            }
            public void reset(String id, String detail) {
                if (eligible(id, observation)) output.reset(id, detail);
            }
        };
        double yaw = Math.abs((float) observation.value("original-delta-yaw"));
        double pitch = Math.abs((float) observation.value("original-delta-pitch"));
        double angle = Math.abs(observation.value("pitch"));
        String detail = "yaw-change=" + yaw + " pitch-change=" + pitch;
        sink.sampleAbove("aim-a", yaw > .5 && scientific(pitch), detail);
        sink.sampleAbove("aim-b", yaw > 0 && ((float) yaw % .25 == 0 || (float) yaw % .1 == 0), detail);
        sink.sampleAbove("aim-e", pitch > .125 && scientific(yaw), detail);
        if (observation.has("original-previous-delta-yaw")) {
            double previous = Math.abs(observation.value("original-previous-delta-yaw"));
            sink.sampleAbove("aim-c", yaw > 1.25 && previous > 1.25 && (float) yaw == (float) previous, detail);
        }
        if (observation.hasAll("target-distance", "target-horizontal")) {
            double distance = observation.value("target-distance");
            double targetMotion = observation.value("target-horizontal");
            sink.sampleAbove("aim-d", yaw > 2.75 && pitch == 0 && angle < 65 && distance > 1 && targetMotion > .03, detail);
            if (eligible("aim-f", observation)) {
                double[] flatPitch = memory.window("combat:aim-f:flat", pitch, 12);
                sink.sampleAbove("aim-f", yaw > 1.825 && pitch == 0 && angle < 60 && distance > 1 && targetMotion > .03
                        && flatPitch.length == 12 && Statistics.range(flatPitch) == 0, detail);
            }
            sink.sampleAbove("aim-g", yaw > 15 && pitch < .1 && angle < 65 && distance > 1, detail);
            sink.sampleAbove("aim-n", yaw > 0 && yaw < .05 && pitch == 0 && targetMotion < .03, detail);
            sink.sampleAbove("aim-o", pitch > 0 && pitch < .05 && yaw == 0 && targetMotion < .03, detail);
        }
        if (observation.hasAll("original-previous-delta-yaw", "original-previous-delta-pitch")) {
            double previousYaw = Math.abs(observation.value("original-previous-delta-yaw"));
            double previousPitch = Math.abs(observation.value("original-previous-delta-pitch"));
            double yawAcceleration = Math.abs(yaw - previousYaw);
            double pitchAcceleration = Math.abs(pitch - previousPitch);
            if (observation.has("target-distance")) {
                sink.sampleAbove("aim-p", yawAcceleration > 20 && pitchAcceleration < .05 && angle < 60
                        && observation.value("target-distance") > 1, detail);
            }
        }
        if (!observation.state("cinematic") && observation.has("sensitivity-mode")) {
            int sensitivityMode = (int) observation.value("sensitivity-mode");
            boolean lowSensitivity = sensitivityMode >= 0 && sensitivityMode < 50;
            if (observation.has("original-previous-delta-pitch")) {
                sink.sampleAbove("aim-i", !lowSensitivity && yaw > .25 && yaw < 20 && pitch > .25 && pitch < 20
                        && scaledRotationGcd((float) pitch, (float) Math.abs(observation.value("original-previous-delta-pitch"))) < 131072, detail);
            }
            if (observation.hasAll("instant-sensitivity", "target-horizontal") && observation.state("target-player")) {
                sink.sampleAbove("aim-h", !lowSensitivity && observation.value("instant-sensitivity") < -1
                        && pitch > 1.25 && pitch < 20 && observation.value("target-horizontal") > .01, detail);
            }
            sink.sampleAbove("aim-w", sensitivityMode < -10 && yaw > 1.25 && pitch > 1.25, detail);
            if (!observation.state("spectator")) sink.sampleAbove("aim-x", sensitivityMode > 205 && yaw > 1.25 && pitch > 1.25, detail);
            if (observation.hasAll("original-previous-delta-yaw", "original-previous-delta-pitch") && yaw > .1f && yaw < 20 && pitch > .1f && pitch < 20) {
                float previousYaw = (float) Math.abs(observation.value("original-previous-delta-yaw"));
                float previousPitch = (float) Math.abs(observation.value("original-previous-delta-pitch"));
                double yawStep = scaledRotationGcd((float) yaw, previousYaw) / (double) (1L << 24);
                double pitchStep = scaledRotationGcd((float) pitch, previousPitch) / (double) (1L << 24);
                double yawModulo = ((float) yaw / yawStep) % (previousYaw / yawStep);
                double pitchModulo = ((float) pitch / pitchStep) % (previousPitch / pitchStep);
                boolean yawPattern = yawModulo > 60 && Math.abs(Math.floor(yawModulo) - yawModulo) > .1;
                boolean pitchPattern = pitchModulo > 60 && Math.abs(Math.floor(pitchModulo) - pitchModulo) > .1;
                sink.sampleAbove("aim-k", (sensitivityMode < 0 || sensitivityMode >= 100) && yawPattern && pitchPattern, detail);
            }
        }
        if (eligible("aim-l", observation) && observation.has("original-signed-delta-pitch")) {
            float signed = (float) observation.value("original-signed-delta-pitch");
            double previous = memory.get("combat:aim-l:direction");
            if (signed * previous < 0 && Math.abs(signed) > 5) sink.sampleAbove("aim-l", true, detail);
            else sink.reset("aim-l", detail);
            memory.set("combat:aim-l:direction", signed);
        }
        if (observation.hasAll("yaw-smoothing-residual", "previous-yaw-smoothing-residual", "attack-horizontal-distance", "teleport-movement-age")
                && !observation.state("dead") && !observation.state("death-grace") && !observation.state("world-change-grace")
                && !observation.state("teleport-exemption") && !observation.state("server-teleport-match") && observation.value("teleport-movement-age") >= 20) {
            float residualChange = Math.abs((float) observation.value("yaw-smoothing-residual") - (float) observation.value("previous-yaw-smoothing-residual"));
            double distance = observation.value("attack-horizontal-distance");
            if (residualChange > 20 && distance > .6 && distance < 10) sink.direct("aim-y", "yaw-residual-change=" + residualChange + " horizontal-distance=" + distance);
        }
        if (!observation.hasAll("mouse-step-yaw", "mouse-step-pitch", "sensitivity-samples")
                || observation.value("sensitivity-samples") < 40) return;
        double yawStep = observation.value("mouse-step-yaw");
        double pitchStep = observation.value("mouse-step-pitch");
        if (!(yawStep > 0) || !(pitchStep > 0)) return;
        if (yawStep < .0096 || yawStep > .6144 || pitchStep < .0096 || pitchStep > .6144) return;
        double yawCount = yaw / yawStep;
        double pitchCount = pitch / pitchStep;
        double yawResidual = Math.abs(yawCount - Math.rint(yawCount));
        double pitchResidual = Math.abs(pitchCount - Math.rint(pitchCount));
        sink.sampleAbove("aim-s", yaw > 0 && pitch > 0 && yawResidual < .0001 && pitchResidual > .03, detail);
        if (observation.hasAll("yaw", "original-previous-delta-yaw", "original-previous-delta-pitch")) {
            double absoluteYawResidual = Math.abs(observation.value("yaw") % yawStep);
            double absolutePitchResidual = Math.abs(observation.value("pitch") % pitchStep);
            sink.sampleAbove("aim-q", yaw + pitch > .75 && absoluteYawResidual < 1e-6 && absolutePitchResidual < 1e-6, detail);
            if (equal(absoluteYawResidual, .01) || equal(absolutePitchResidual, .01)) sink.direct("aim-u", detail);
        }
    }

    private static boolean scientific(double value) {
        return value < 1 && Double.toString((double) (float) value).contains("E");
    }

    private static boolean eligible(String id, Observation observation) {
        int acceptedLimit = switch (id) {
            case "aim-g", "aim-o" -> 12;
            case "aim-c", "aim-d", "aim-f", "aim-l", "aim-n", "aim-p" -> 18;
            default -> 0;
        };
        if (acceptedLimit > 0 && (!observation.has("accepted-attack-age-ticks")
                || observation.value("accepted-attack-age-ticks") >= acceptedLimit
                || observation.value("attack-movement-age") >= acceptedLimit / 6)) return false;
        return switch (id) {
            case "aim-e", "aim-f", "aim-g" -> !observation.state("vehicle") && !observation.state("vehicle-grace");
            case "aim-n" -> !observation.state("vehicle") && !observation.state("vehicle-grace") && !observation.state("nearby-boat");
            case "aim-q" -> !observation.state("creative") && !observation.state("spectator");
            default -> true;
        };
    }

    private static boolean equal(double first, double second) { return Math.abs(first - second) < 1e-6; }
    private static long scaledRotationGcd(float pitch, float previousPitch) {
        long first = (long) (pitch * (double) (1L << 24));
        long second = (long) (previousPitch * (double) (1L << 24));
        while (second > 16384) {
            long remainder = first % second;
            first = second;
            second = remainder;
        }
        return first;
    }
}
