package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CombatRules implements RuleModule {
    private final List<RuleModule> modules = List.of(new AimRules(), new ClickRules(), new SequenceCombatRules());
    private final Set<String> ids;

    public CombatRules() {
        Set<String> collected = new HashSet<>();
        for (RuleModule module : modules) {
            for (String id : module.checkIds()) if (!collected.add(id)) throw new IllegalStateException("Duplicate combat check: " + id);
        }
        ids = Set.copyOf(collected);
    }

    public Set<String> checkIds() { return ids; }

    public void evaluate(Observation observation, DetectionMemory memory, FindingSink sink) {
        for (RuleModule module : modules) module.evaluate(observation, memory, sink);
    }
}
