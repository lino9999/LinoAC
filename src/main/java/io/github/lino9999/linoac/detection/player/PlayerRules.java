package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PlayerRules implements RuleModule {
    private final PacketRules packets = new PacketRules();
    private final List<RuleModule> modules = List.of(packets, new InvalidRules(), new GroundSpoofRules(), new TimerRules());
    private final Set<String> ids;

    public PlayerRules() {
        Set<String> collected = new LinkedHashSet<>();
        modules.forEach(module -> collected.addAll(module.checkIds()));
        ids = Set.copyOf(collected);
    }

    @Override
    public Set<String> checkIds() { return ids; }

    @Override
    public void evaluate(Observation observation, DetectionMemory memory, FindingSink sink) {
        if (observation.state("source-profile-incomplete")) { packets.evaluateStateless(observation, sink); return; }
        modules.forEach(module -> module.evaluate(observation, memory, sink));
    }
}
