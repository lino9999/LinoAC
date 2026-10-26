package io.github.lino9999.linoac.detection.world;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import java.util.Set;

public final class WorldRules implements RuleModule {
    private static final Set<String> IDS = Set.of("airplace-a", "baritone-a", "baritone-b", "fastbreak-a", "fastplace-a", "ghosthand-a", "tower-a",
            "scaffold-a", "scaffold-b", "scaffold-c", "scaffold-d", "scaffold-e", "scaffold-f", "scaffold-g", "scaffold-h", "scaffold-i", "scaffold-j", "scaffold-k", "scaffold-m", "scaffold-n",
            "improbable-a", "improbable-b", "improbable-c", "improbable-d", "improbable-e", "improbable-f");

    private final PlacementPatterns patterns = new PlacementPatterns();

    @Override
    public Set<String> checkIds() { return IDS; }

    public static boolean ownsExemptions(String id) { return PlacementPatterns.IDS.contains(id); }

    @Override
    public void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        combinations(o, memory, sink);
        patterns.evaluate(o, memory, sink);
        if (o.kind() == Observation.Kind.BREAK && o.state("bukkit-event") && o.state("fastbreak-callback")) breaking(o, sink);
        if (o.kind() == Observation.Kind.BREAK && o.state("bukkit-event") && o.state("ghosthand-callback")) ghostHand(o, sink);
        if (o.kind() == Observation.Kind.PLACE && o.state("bukkit-event")) {
            fastPlace(o, memory, sink);
            scaffoldN(o, memory, sink);
            scaffoldJ(o, sink);
        }
    }

    private void fastPlace(Observation o, DetectionMemory memory, FindingSink sink) {
        if (!o.state("block-accepted") || !o.state("fastplace-material-known") || o.state("fastplace-material-exempt")
                || o.state("held-hoe") || o.state("vehicle") || o.state("vehicle-grace")
                || !o.hasAll("event-time-ms", "cancelled-break-age-ticks") || !Double.isFinite(o.value("cancelled-break-age-ticks"))
                || o.value("cancelled-break-age-ticks") < 40) return;
        double now = o.value("event-time-ms");
        if (!Double.isFinite(now) || now < 0 || now != Math.rint(now)) return;
        double last = memory.get("world.fastplace-time");
        if (now < last) memory.set("world.fastplace-count", 0);
        double count = Math.min(Integer.MAX_VALUE, memory.get("world.fastplace-count") + 1);
        memory.set("world.fastplace-count", count);
        if (count == 1) memory.set("world.fastplace-time", now);
        if (now - memory.get("world.fastplace-time") > 1000) {
            if (count > o.value("placement-limit", 15)) sink.direct("fastplace-a", "Placed blocks in source batch=" + count);
            memory.set("world.fastplace-count", 0);
            memory.set("world.fastplace-time", 0);
        }
    }

    private void scaffoldJ(Observation o, FindingSink sink) {
        if (!Boolean.TRUE.equals(PlacementContext.bridge(o)) || !o.hasAll("horizontal", "previous-horizontal", "original-delta-pitch")) return;
        double acceleration = Math.abs(o.value("horizontal") - o.value("previous-horizontal"));
        if (scientific(acceleration) && o.value("original-delta-pitch") > 18) sink.direct("scaffold-j", "Pitch snap with scientific notation horizontal acceleration=" + acceleration);
    }

    private void scaffoldN(Observation o, DetectionMemory memory, FindingSink sink) {
        if (!o.state("block-accepted") || !o.state("scaffold-n-material-known") || o.state("scaffold-n-material-exempt")
                || !o.hasAll("block-x", "block-y", "block-z", "against-x", "against-y", "against-z", "world-token")) return;
        for (String key : new String[]{"block-x", "block-y", "block-z", "against-x", "against-y", "against-z", "world-token"}) {
            if (!Double.isFinite(o.value(key)) || o.value(key) != Math.rint(o.value(key))) return;
        }
        int count = (int) memory.get("world.n.chain-count");
        boolean linked = false;
        for (int i = 0; i < count; i++) {
            String key = "world.n.chain:" + i;
            if (memory.get(key + ":world") == o.value("world-token") && memory.get(key + ":x") == o.value("against-x")
                    && memory.get(key + ":y") == o.value("against-y") && memory.get(key + ":z") == o.value("against-z")) {
                linked = true;
                break;
            }
        }
        if (linked) {
            if (!o.state("block-two-below-known") || !o.hasAll("x", "y", "z", "block-face")) return;
            double face = o.value("block-face");
            if (face < 2 || face > 5 || face != Math.rint(face)) return;
            double distance = Math.hypot(o.value("block-x") - o.value("x"), o.value("block-z") - o.value("z"));
            boolean aligned = Math.floor(o.value("x")) == o.value("block-x") || Math.floor(o.value("z")) == o.value("block-z");
            boolean candidate = o.state("block-two-below-air") && o.value("block-y") < o.value("y") && distance > 1.85 && aligned;
            if (candidate && !o.state("source-flight") && !o.state("source-creative") && o.has("feet-solid-samples")) {
                double samples = o.value("feet-solid-samples");
                if (Double.isFinite(samples) && samples >= 0 && samples <= 4 && samples == Math.rint(samples)) {
                    sink.sampleAbove("scaffold-n", samples > 0, "Expanded linked placement distance=" + distance + " solid feet samples=" + samples);
                }
            }
            memory.set("world.n.chain-count", 0);
            memory.set("world.n.chain-next", 0);
        }
        int next = (int) memory.get("world.n.chain-next");
        String key = "world.n.chain:" + next;
        memory.set(key + ":world", o.value("world-token"));
        memory.set(key + ":x", o.value("block-x"));
        memory.set(key + ":y", o.value("block-y"));
        memory.set(key + ":z", o.value("block-z"));
        memory.set("world.n.chain-count", Math.min(512, memory.get("world.n.chain-count") + 1));
        memory.set("world.n.chain-next", (next + 1) % 512);
    }

    private void breaking(Observation o, FindingSink sink) {
        if (!o.state("block-accepted") || !o.state("fastbreak-context-known") || !o.state("source-survival")
                || o.state("fastbreak-material-exempt") || o.state("fastbreak-tool-exempt") || o.state("fastbreak-ability")
                || o.state("mcmmo-berserk") || o.state("vehicle") || o.state("vehicle-grace") || !o.has("mining-deadline-difference-ms")) return;
        double difference = o.value("mining-deadline-difference-ms");
        if (Double.isFinite(difference)) sink.sampleAbove("fastbreak-a", difference < o.value("minimum-break-difference", -125), "Mining deadline difference-ms=" + difference);
    }

    private void ghostHand(Observation o, FindingSink sink) {
        if (!o.state("ghosthand-context-known") || !o.hasAll("ghosthand-wire-y", "block-y", "ghosthand-opaque-blocks", "ghosthand-glass-blocks")) return;
        double wireY = o.value("ghosthand-wire-y"), blockY = o.value("block-y");
        double opaque = o.value("ghosthand-opaque-blocks"), glass = o.value("ghosthand-glass-blocks");
        if (!Double.isFinite(wireY) || !Double.isFinite(blockY) || !Double.isFinite(opaque) || !Double.isFinite(glass)
                || opaque < 0 || opaque > 10 || opaque != Math.rint(opaque) || glass < 0 || glass > 8 || glass != Math.rint(glass) || wireY < blockY) return;
        if (opaque == 10 || glass == 8) sink.direct("ghosthand-a", "Enclosed bed: opaque blocks=" + opaque + " glass blocks=" + glass);
    }

    private void combinations(Observation o, DetectionMemory memory, FindingSink sink) {
        String[] groups = {"combat", "movement", "player", "autoclicker", "total", "scaffold"};
        double[] defaults = {25, 30, 30, 30, 50, 25};
        for (int i = 0; i < groups.length; i++) {
            boolean eligible = switch (i) {
                case 0 -> o.kind() == Observation.Kind.ATTACK;
                case 1, 2, 5 -> o.kind() == Observation.Kind.MOVE && o.state("position-packet");
                case 3 -> o.kind() == Observation.Kind.SWING && o.state("click-context-known") && !o.state("click-statistical-exempt");
                default -> o.kind() == Observation.Kind.MOVE;
            };
            if (!eligible) continue;
            String group = groups[i];
            if (!o.hasAll("violations-" + group, "packet-time-ms")) continue;
            String id = "improbable-" + (char) ('a' + i);
            double now = o.value("packet-time-ms");
            double last = memory.get("world.combination-time." + id);
            if (!Double.isFinite(now) || now - last < 60000) continue;
            double count = o.value("violations-" + group);
            boolean excessive = count > o.value("improbable-limit-" + group, defaults[i]);
            if (Double.isFinite(count) && excessive) {
                sink.direct(id, "Combined " + group + " violations=" + count);
                memory.set("world.combination-time." + id, now);
            }
        }
    }

    private static boolean scientific(double value) { return value > 0 && value < 1 && Double.toString(value).contains("E"); }
}
