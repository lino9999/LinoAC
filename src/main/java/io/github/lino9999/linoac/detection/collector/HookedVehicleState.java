package io.github.lino9999.linoac.detection.collector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class HookedVehicleState {
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private final int maximum;
    private final long grace;
    private boolean complete = true;
    private long lastPrune = Long.MIN_VALUE;

    public HookedVehicleState(int maximum, long grace) {
        if (maximum < 1 || grace < 0) throw new IllegalArgumentException("Invalid hook tracking bounds.");
        this.maximum = maximum; this.grace = grace;
    }

    public void caught(UUID vehicle, UUID hook, long tick) {
        entries.entrySet().removeIf(entry -> !entry.getValue().noHooksKnown && entry.getValue().lastSeen + grace < tick && entry.getValue().hooks.isEmpty());
        if (!entries.containsKey(vehicle) && entries.size() >= maximum) { complete = false; entries.remove(entries.keySet().iterator().next()); }
        var entry = entries.computeIfAbsent(vehicle, ignored -> new Entry());
        if (entry.hooks.size() >= 4 && !entry.hooks.containsKey(hook)) { complete = false; entry.hooks.remove(entry.hooks.keySet().iterator().next()); }
        entry.hooks.put(hook, tick); entry.lastSeen = tick; entry.noHooksKnown = false;
    }

    public Boolean hooked(UUID vehicle, long tick, Predicate<UUID> valid) {
        var entry = entries.get(vehicle);
        if (entry == null) return complete ? Boolean.FALSE : null;
        boolean hadHooks = !entry.hooks.isEmpty();
        entry.hooks.keySet().removeIf(hook -> !valid.test(hook));
        if (hadHooks && entry.hooks.isEmpty()) { entry.lastSeen = tick; entry.noHooksKnown = true; }
        if (!entry.hooks.isEmpty()) entry.lastSeen = tick;
        if (tick - entry.lastSeen <= grace) return true;
        if (entry.noHooksKnown) return false;
        entries.remove(vehicle); return complete ? Boolean.FALSE : null;
    }

    public void confirmedAbsence(UUID vehicle, long tick) {
        if (!entries.containsKey(vehicle) && entries.size() >= maximum) { complete = false; entries.remove(entries.keySet().iterator().next()); }
        Entry entry = entries.computeIfAbsent(vehicle, ignored -> new Entry());
        if (!entry.noHooksKnown) { entry.lastSeen = tick; entry.noHooksKnown = true; }
    }

    public void prune(long tick, Predicate<UUID> valid) {
        prune(tick, valid, ignored -> true);
    }

    public void prune(long tick, Predicate<UUID> valid, Predicate<UUID> validVehicle) {
        if (lastPrune != Long.MIN_VALUE && tick - lastPrune < 200) return;
        lastPrune = tick;
        entries.entrySet().removeIf(pair -> {
            Entry entry = pair.getValue();
            if (!validVehicle.test(pair.getKey())) return true;
            boolean hadHooks = !entry.hooks.isEmpty();
            entry.hooks.keySet().removeIf(hook -> !valid.test(hook));
            if (hadHooks) { entry.lastSeen = tick; if (entry.hooks.isEmpty()) entry.noHooksKnown = true; }
            return entry.hooks.isEmpty() && !entry.noHooksKnown && tick - entry.lastSeen > grace;
        });
    }

    public void clear() { entries.clear(); complete = true; lastPrune = Long.MIN_VALUE; }
    public int size() { return entries.size(); }
    private static final class Entry {
        private final Map<UUID, Long> hooks = new LinkedHashMap<>();
        private long lastSeen;
        private boolean noHooksKnown;
    }
}
