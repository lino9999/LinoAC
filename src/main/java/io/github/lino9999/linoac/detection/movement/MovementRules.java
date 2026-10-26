package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;

import java.util.Set;

public final class MovementRules implements RuleModule {
    private static final Set<String> IDS = Set.of(
            "antilevitation-a", "boatfly-a", "boatfly-b", "boatfly-c",
            "elytra-a", "elytra-b", "elytra-c", "elytra-f", "elytra-g", "elytra-i", "elytra-k", "elytra-l", "elytra-m", "elytra-n",
            "entityflight-a", "entityflight-b", "entityspeed-a", "fastclimb-a",
            "flight-a", "flight-b", "flight-c", "flight-d", "flight-e", "flight-f",
            "jesus-a", "jesus-b", "jesus-c", "jesus-d", "jesus-e", "jump-a", "jump-b", "jump-c",
            "motion-a", "motion-b", "motion-c", "motion-e", "nosaddle-a", "noslow-a", "noslow-b", "noslow-c",
            "speed-a", "speed-b", "speed-c", "speed-d", "speed-e", "sprint-a", "step-a", "step-c", "strafe-a", "vclip-a", "wallclimb-a");

    private final PositionRules positions = new PositionRules();
    private final SurfaceRules surfaces = new SurfaceRules();
    private final MountedRules mounted = new MountedRules();
    private final GlidingRules gliding = new GlidingRules();
    private final HeadingRules heading = new HeadingRules();
    private final HorizontalBudgetRules horizontal = new HorizontalBudgetRules();

    public Set<String> checkIds() { return IDS; }
    public static boolean ownsExemptions(String id) { return IDS.contains(id); }

    public void evaluate(Observation input, DetectionMemory memory, FindingSink sink) {
        positions.evaluate(input, memory, sink);
        surfaces.evaluate(input, memory, sink);
        mounted.evaluate(input, memory, sink);
        gliding.evaluate(input, memory, sink);
        heading.evaluate(input, memory, sink);
        horizontal.evaluate(input, memory, sink);
        if (input.kind() == Observation.Kind.RELEASE && input.state("dig:RELEASE_USE_ITEM") && input.has("placements-since-move")) {
            sink.sample("noslow-a", input.state("legacy-server") && input.state("legacy-client") && input.value("placements-since-move") > 0,
                    "release-after-placement-before-movement");
        }
    }
}
