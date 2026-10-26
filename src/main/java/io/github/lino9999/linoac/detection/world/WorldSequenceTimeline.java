package io.github.lino9999.linoac.detection.world;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;

final class WorldSequenceTimeline {
    enum Clock { INTERACT, BRIDGE_FACE, SPRINT_PLACE, SNEAK_PLACE, DIRECTION_PLACE, TOWER_PLACE }
    enum Window { PITCH_PATTERN, ROTATION_SNAP, SENSITIVITY }
    private final DetectionMemory memory;

    WorldSequenceTimeline(DetectionMemory memory) { this.memory = memory; }

    void observe(Observation observation) {
        if (observation.kind() == Observation.Kind.DIG && observation.state("dig:CANCELLED_DIGGING")) {
            memory.set("world.sequence.small-age", 0);
            memory.set("world.sequence.large-age", 0);
        }
        if (!observation.state("bukkit-event") && (observation.state("action:START_SNEAKING") || observation.state("action:STOP_SNEAKING"))) {
            memory.set("world.sequence.sneak-pending", 1);
            memory.set("world.sequence.sneak-tick", observation.tick());
        }
        if (observation.kind() == Observation.Kind.MOVE) {
            double bridge = memory.get("world.sequence.bridge");
            double age = memory.get("world.sequence.bridge-age", 100);
            memory.set("world.sequence.bridge-age", bridge == 1 ? 0 : bridge < 0 || age < 0 ? -1 : increment(age));
            advance(Window.ROTATION_SNAP);
            if (observation.state("position-packet")) {
                if (observation.has("source-sprinting")) memory.set("world.sequence.sprint-age", observation.value("source-sprinting") == 1
                        ? increment(memory.get("world.sequence.sprint-age")) : 0);
                else memory.set("world.sequence.sprint-age", -1);
            }
        }
        if (observation.kind() == Observation.Kind.ROTATION) {
            memory.set("world.sequence.small-age", increment(memory.get("world.sequence.small-age")));
            memory.set("world.sequence.large-age", increment(memory.get("world.sequence.large-age", 100)));
            advance(Window.PITCH_PATTERN);
            advance(Window.SENSITIVITY);
        }
        if (observation.state("world-item-frame-interaction") && observation.has("event-time-ms")) commit(Clock.INTERACT, observation.value("event-time-ms"));
    }

    void placement(Boolean bridge, boolean accepted, double now) {
        memory.set("world.sequence.bridge", bridge == null ? -1 : bridge ? 1 : 0);
        if (!accepted) return;
        for (Window window : Window.values()) {
            String prefix = prefix(window);
            memory.set(prefix + "interval", now - memory.get(prefix + "time"));
            memory.set(prefix + "time", now);
            memory.set(prefix + "age", 0);
            memory.set(prefix + "bridge", bridge == null ? -1 : bridge ? 1 : 0);
        }
    }

    boolean window(Window window, int maximum) {
        String prefix = prefix(window);
        return memory.get(prefix + "bridge") == 1 && memory.get(prefix + "age") < maximum
                && memory.get(prefix + "interval") >= 0 && memory.get(prefix + "interval") < 400;
    }

    double smallRotationAge() { return memory.get("world.sequence.small-age"); }
    double largeRotationAge() { return memory.get("world.sequence.large-age", 100); }
    double bridgeAge() { return memory.get("world.sequence.bridge-age", 100); }
    double sprintAge() { return memory.get("world.sequence.sprint-age"); }
    boolean sneakPending() { return memory.get("world.sequence.sneak-pending") == 1; }
    void consumeSneak() { memory.set("world.sequence.sneak-pending", 0); }
    boolean sneakGrace(long tick) { return tick - memory.get("world.sequence.sneak-tick") <= 20; }
    double elapsed(Clock clock, double now) { return now - memory.get(clockKey(clock)); }
    void commit(Clock clock, double now) { memory.set(clockKey(clock), now); }

    Integer batch(double now) {
        double first = memory.get("world.sequence.batch-first");
        double count = memory.get("world.sequence.batch-count");
        if (count > 0 && now < first) count = 0;
        if (count == 0) { first = now; memory.set("world.sequence.batch-first", now); }
        count = increment(count);
        memory.set("world.sequence.batch-count", count);
        if (now - first <= 1000) return null;
        memory.set("world.sequence.batch-count", 0);
        memory.set("world.sequence.batch-first", 0);
        return (int) count;
    }

    private void advance(Window window) { String key = prefix(window) + "age"; memory.set(key, increment(memory.get(key))); }
    private static double increment(double value) { return Math.min(Integer.MAX_VALUE, value + 1); }
    private static String prefix(Window window) { return "world.sequence.window." + window.name() + "."; }
    private static String clockKey(Clock clock) { return "world.sequence.clock." + clock.name(); }
}
