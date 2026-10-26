package io.github.lino9999.linoac.detection.world;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import io.github.lino9999.linoac.detection.Statistics;
import java.util.Arrays;
import java.util.Set;

public final class WorldRules implements RuleModule {
    private static final Set<String> IDS = Set.of("airplace-a", "baritone-a", "baritone-b", "fastbreak-a", "fastplace-a", "ghosthand-a", "tower-a",
            "scaffold-a", "scaffold-b", "scaffold-c", "scaffold-d", "scaffold-e", "scaffold-f", "scaffold-g", "scaffold-h", "scaffold-i", "scaffold-j", "scaffold-k", "scaffold-m", "scaffold-n",
            "improbable-a", "improbable-b", "improbable-c", "improbable-d", "improbable-e", "improbable-f");

    @Override
    public Set<String> checkIds() { return IDS; }

    @Override
    public void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        combinations(o, memory, sink);
        if (o.kind() == Observation.Kind.BREAK && o.state("bukkit-event") && o.state("fastbreak-callback")) breaking(o, sink);
        if (o.kind() == Observation.Kind.BREAK && o.state("bukkit-event") && o.state("ghosthand-callback")) ghostHand(o, sink);
        if (o.kind() == Observation.Kind.PLACE && o.state("bukkit-event")) {
            fastPlace(o, memory, sink);
            scaffoldN(o, memory, sink);
            scaffoldJ(o, sink);
        }
        if (o.state("teleport-grace") || o.state("creative") || o.state("spectator") || o.state("block-unloaded")) return;
        if (o.kind() == Observation.Kind.MOVE) {
            memory.set("world.sprint-ticks", o.state("sprinting") ? memory.get("world.sprint-ticks") + 1 : 0);
            if (memory.get("world.bridge-active") > 0) {
                double moves = memory.increment("world.bridge-moves");
                if (moves < 3 && memory.get("world.bridge-interval", 1000) < 400 && o.hasAll("delta-yaw", "delta-pitch")) {
                    sink.sample("scaffold-e", Math.abs(o.value("delta-yaw")) > 100 && Math.abs(o.value("delta-pitch")) > 8, "Large rotation immediately followed accepted bridging placement");
                }
            }
        }
        if (o.kind() == Observation.Kind.DIG && o.state("dig:CANCELLED_DIGGING")) memory.set("world.cancelled-dig-rotations", 0);
        if (o.state("action:START_SNEAKING") || o.state("action:STOP_SNEAKING")) memory.set("world.sneak-toggle", 1);
        if (o.kind() == Observation.Kind.ROTATION) rotations(o, memory, sink);
        if (o.kind() == Observation.Kind.USE && o.state("block-interaction") && o.has("block-face")) {
            sink.sample("scaffold-a", o.state("clicked-below-feet") && o.state("clicked-solid") && o.state("held-solid") && o.value("block-face") == 0, "Downward interaction with the solid block below the player");
        }
        if (o.kind() == Observation.Kind.PLACE && o.state("block-accepted") && o.hasAll("block-x", "block-y", "block-z", "x", "y", "z", "block-face")) placement(o, memory, sink);
    }

    private void placement(Observation o, DetectionMemory memory, FindingSink sink) {
        double last = memory.get("world.place-time", -1);
        double interval = last < 0 ? Double.POSITIVE_INFINITY : (o.nanos() - last) / 1_000_000d;
        memory.set("world.place-time", o.nanos());
        if (o.state("block-support-known") && o.hasAll("against-x", "against-y", "against-z")) {
            boolean selfSupport = o.value("against-x") == o.value("block-x") && o.value("against-y") == o.value("block-y") && o.value("against-z") == o.value("block-z");
            sink.sample("airplace-a", selfSupport && !o.state("block-supported") && !o.state("placement-exempt") && !o.state("block-break-grace"), "Placement referenced itself while all neighboring blocks were air");
        }
        double dx = o.value("block-x") - o.value("x");
        double dz = o.value("block-z") - o.value("z");
        double expand = Math.hypot(dx, dz);
        boolean bridging = Boolean.TRUE.equals(PlacementContext.bridge(o));
        memory.set("world.bridge-active", bridging ? 1 : 0);
        memory.set("world.bridge-interval", interval);
        memory.set("world.bridge-rotations", 0);
        memory.set("world.bridge-moves", 0);
        boolean sneakToggle = memory.get("world.sneak-toggle") > 0;
        memory.set("world.sneak-toggle", 0);
        if (o.state("placement-exempt")) return;
        double face = o.value("block-face");
        boolean ordinaryPlacement = o.value("block-y") < o.value("y") && o.state("placed-solid") && !o.state("liquid") && !o.state("environment-special") && !o.state("allow-flight");
        if (ordinaryPlacement) {
            sink.sample("scaffold-c", o.state("sprinting") && memory.get("world.sprint-ticks") > 5 && interval >= 0 && interval < 300, "Sustained sprinting during rapid accepted placements");
            if (o.has("yaw")) sink.sample("scaffold-m", expand > 1 && expand < 6 && face == facing(o.value("yaw")), "Placement expanded forward along the yaw cardinal face by=" + expand);
            if (o.hasAll("dy", "horizontal") && !o.state("movement-grace") && !o.state("jump-effect") && !o.state("slow-falling") && !o.state("climbing") && !o.state("explosion") && !o.state("gliding")) {
                boolean sameColumn = Math.floor(o.value("x")) == o.value("block-x") && Math.floor(o.value("z")) == o.value("block-z");
                sink.sample("tower-a", sameColumn && face == 1 && interval >= 0 && interval < 250 && o.value("horizontal") < .1 && o.value("dy") > 0, "Rapid accepted placements underneath upward tower movement");
            }
        }
        if (!bridging) return;
        sink.sample("scaffold-b", face == 6 && interval >= 0 && interval < 300, "Placement used the clicked block itself as the attachment face");
        if (!o.state("swift-sneak") && !o.state("slab")) {
            double[] bridge = memory.window("world.bridge-placements", o.nanos() / 1_000_000d, 32);
            long bridgeRate = recent(bridge, o.nanos());
            sink.sample("scaffold-k", bridgeRate >= o.value("bridge-placement-limit", 6), "Accepted bridging placements per second=" + bridgeRate);
        }
        if (o.hasAll("horizontal", "movement-speed") && o.state("physics-known") && !o.state("movement-grace") && !o.state("vehicle")) {
            double speedLimit = .27 * Math.max(1, o.value("movement-speed") / .1);
            sink.sample("scaffold-f", sneakToggle && interval >= 0 && interval < 400 && o.value("horizontal") > speedLimit, "Sneak transition followed rapid bridging above the movement attribute speed budget");
        }
        if (o.hasAll("horizontal", "previous-horizontal") && !o.state("movement-grace")) {
            double acceleration = Math.abs(o.value("horizontal") - o.value("previous-horizontal"));
            if (o.hasAll("dx", "dz", "yaw") && !o.state("swift-sneak") && !o.state("slab") && o.value("horizontal") > 0) {
                double angle = movementAngle(o.value("dx"), o.value("dz"), o.value("yaw"));
                sink.sample("scaffold-g", interval >= 0 && interval < 300 && acceleration > 0 && acceleration < .001 && angle < 2, "Constant bridging acceleration with movement direction angle=" + angle);
            }
            if (o.hasAll("delta-yaw", "delta-pitch")) {
                double yaw = Math.abs(o.value("delta-yaw"));
                double pitch = Math.abs(o.value("delta-pitch"));
                sink.sample("scaffold-i", acceleration < .01 && yaw > 150 && pitch > 10, "Large bridging rotation with unchanged horizontal speed");
            }
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

    private void rotations(Observation o, DetectionMemory memory, FindingSink sink) {
        if (!o.hasAll("delta-yaw", "delta-pitch")) return;
        double yaw = Math.abs(o.value("delta-yaw"));
        double pitch = Math.abs(o.value("delta-pitch"));
        double sinceCancel = memory.get("world.cancelled-dig-rotations", 100) + 1;
        memory.set("world.cancelled-dig-rotations", sinceCancel);
        if (sinceCancel < 10) sink.sample("baritone-a", yaw > 0 && yaw < .005 && pitch > 0 && pitch < .005, "Small positive yaw and pitch rotations shortly after cancelled digging");
        if (sinceCancel < 5 && o.has("pitch")) sink.sample("baritone-b", yaw > 95 && pitch < .15 && Math.abs(o.value("pitch")) < 70 && !o.state("piston"), "Large yaw with nearly fixed pitch shortly after cancelled digging");
        double rotations = memory.increment("world.bridge-rotations");
        if (memory.get("world.bridge-active") == 0 || memory.get("world.bridge-interval", 1000) >= 400 || o.state("movement-grace") || o.state("cinematic") || o.state("swift-sneak")) return;
        if (rotations < 6 && o.has("previous-delta-pitch")) {
            double pitchGcd = Statistics.gcd(pitch, Math.abs(o.value("previous-delta-pitch")));
            sink.sample("scaffold-d", pitch > .5 && pitchGcd > 0 && pitchGcd < .0078125, "Bridging pitch rotation quantum=" + pitchGcd);
        }
        if (rotations < 5 && o.hasAll("mouse-step-pitch", "sensitivity-samples", "horizontal") && o.value("sensitivity-samples") >= 40 && o.value("mouse-step-pitch") > 0) {
            double sensitivity = (Math.cbrt(o.value("mouse-step-pitch") / 1.2) - .2) / .6 * 200;
            sink.sample("scaffold-h", (sensitivity < -2 || sensitivity > 200) && pitch > .55 && o.value("horizontal") > .25, "Bridging pitch sensitivity outside the legal client range=" + sensitivity);
        }
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

    private static long recent(double[] times, long now) {
        double millis = now / 1_000_000d;
        return Arrays.stream(times).filter(time -> millis >= time && millis - time < 1000).count();
    }

    private static boolean scientific(double value) { return value > 0 && value < 1 && Double.toString(value).contains("E"); }

    private static double movementAngle(double dx, double dz, double yaw) {
        double length = Math.hypot(dx, dz);
        if (length == 0) return Math.PI;
        double radians = Math.toRadians(yaw);
        double cosine = (dx * -Math.sin(radians) + dz * Math.cos(radians)) / length;
        return Math.acos(Math.max(-1, Math.min(1, cosine)));
    }

    private static int facing(double yaw) {
        int quadrant = Math.floorMod((int) Math.floor(yaw / 90 + .5), 4);
        return switch (quadrant) { case 0 -> 3; case 1 -> 4; case 2 -> 2; default -> 5; };
    }
}
