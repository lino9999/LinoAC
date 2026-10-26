package io.github.lino9999.linoac.detection;

import java.util.Set;

public final class CheckExemptions {
    private static final Set<String> JOINED = Set.of("antilevitation-a", "badpackets-t", "elytra-n", "groundspoof-a", "groundspoof-c",
            "invalid-c", "invalid-e", "invalid-f", "invalid-i", "invalid-j", "jump-b", "motion-b", "motion-e", "reach-b",
            "speed-a", "speed-b", "speed-c", "speed-d", "sprint-a", "step-a", "step-c", "strafe-a", "timer-a", "timer-d", "vclip-a");
    private static final Set<String> TELEPORT = Set.of("aim-y", "criticals-a", "criticals-b", "elytra-a", "elytra-l", "elytra-n",
            "fastclimb-a", "flight-f", "groundspoof-a", "groundspoof-b", "groundspoof-c", "invalid-c", "invalid-e", "invalid-f",
            "invalid-i", "invalid-j", "jump-b", "motion-a", "motion-b", "motion-c", "motion-e", "noslow-b", "noslow-c", "reach-b",
            "scaffold-a", "speed-b", "speed-c", "speed-d", "speed-e", "sprint-a", "step-a", "step-c", "strafe-a", "vclip-a", "velocity-d");

    private CheckExemptions() {}

    public static boolean exempt(String id, Observation observation) {
        if (io.github.lino9999.linoac.detection.movement.MovementRules.ownsExemptions(id)
                || io.github.lino9999.linoac.detection.world.WorldRules.ownsExemptions(id)) return false;
        if (id.equals("timer-a") || id.equals("timer-d") || id.startsWith("badpackets-") || id.startsWith("invalid-") || id.startsWith("groundspoof-")) return false;
        if (observation.state("sequence-context-known") && observation.state("source-" + id + "-known")
                && (id.equals("criticals-a") || id.equals("criticals-b") || id.equals("reach-b") || id.equals("velocity-d"))) return false;
        return observation.state("joined") && JOINED.contains(id)
                || observation.state("teleport-exemption") && TELEPORT.contains(id);
    }

    public static boolean joined(long elapsedMillis, long movementPackets, long waitMillis, long minimumMovementPackets) {
        if (waitMillis < 0 || minimumMovementPackets < 0) throw new IllegalArgumentException("Join limits must be nonnegative.");
        return elapsedMillis < waitMillis || movementPackets < minimumMovementPackets;
    }
}
