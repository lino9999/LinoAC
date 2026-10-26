package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;

import java.util.Set;

final class SequenceCombatRules implements RuleModule {
    private static final Set<String> IDS = Set.of("autoblock-a", "autoblock-b", "autoblock-c", "autoblock-d", "criticals-a", "criticals-b",
            "fastbow-a", "hitbox-a", "hitbox-b", "killaura-a", "killaura-b", "killaura-c", "killaura-d", "killaura-j", "killaura-k", "killaura-l",
            "reach-a", "reach-b", "velocity-a", "velocity-b", "velocity-d");

    public Set<String> checkIds() { return IDS; }

    public void evaluate(Observation observation, DetectionMemory memory, FindingSink sink) {
        if (!observation.state("sequence-context-known")) return;
        if (observation.kind() == Observation.Kind.ATTACK) attack(observation, sink);
        if (observation.state("sequence-autoblock-d-ready") && observation.state("legacy-client")) {
            sink.direct("autoblock-d", "placement-after-attack-without-interaction");
        }
        if (observation.state("sequence-entity-event")) criticals(observation, sink);
        if (observation.state("source-arrow-launch")) bow(observation, sink);
        if (observation.kind() == Observation.Kind.MOVE) {
            movement(observation, sink);
            reach(observation, sink);
            velocity(observation, memory, sink);
        }
    }

    private void attack(Observation input, FindingSink sink) {
        if (input.state("legacy-client")) {
            if (input.value("sequence-autoblock-a") != 0) sink.direct("autoblock-a", "attack-after-wire-placement");
            if (input.value("sequence-autoblock-b") != 0) sink.direct("autoblock-b", "attack-after-use-release");
        }
        if (input.has("sequence-autoblock-c")) {
            sink.sampleAbove("autoblock-c", input.value("sequence-autoblock-c") != 0 && input.state("blocking-weapon")
                    && input.state("legacy-client"), "attack-after-dig-with-sword");
        }
        if (input.has("sequence-killaura-d-target-changes")) sink.direct("killaura-d", "target-changes=" + input.value("sequence-killaura-d-target-changes"));
        if (input.has("sequence-killaura-j-equal-count")) {
            double equal = input.value("sequence-killaura-j-equal-count");
            sink.sampleAbove("killaura-j", equal > 28, "equal-movement-intervals=" + equal + "/30");
        }
        if (input.has("sequence-killaura-k-range-ms")) {
            double range = input.value("sequence-killaura-k-range-ms");
            sink.sampleAbove("killaura-k", range < 50, "attack-interval-range-ms=" + range);
        }
    }

    private void movement(Observation input, FindingSink sink) {
        if (input.has("sequence-killaura-a-ms")) {
            double elapsed = input.value("sequence-killaura-a-ms");
            sink.sampleAbove("killaura-a", elapsed > 40 && elapsed < 100 && !input.state("spectator"), "post-attack-frame-ms=" + elapsed);
        }
        if (!input.state("position-packet") || !input.has("attack-movement-age") || input.value("attack-movement-age") >= 3) return;
        if (input.state("source-killaura-c-known") && input.hasAll("source-horizontal", "source-previous-horizontal", "original-delta-yaw", "original-delta-pitch")) {
            double horizontal = input.value("source-horizontal");
            double acceleration = Math.abs(horizontal - input.value("source-previous-horizontal"));
            sink.sampleAbove("killaura-c", acceleration < .001 && horizontal > 0 && input.value("original-delta-yaw") > 10
                    && input.value("original-delta-pitch") > 26.5 && input.state("target-player") && !input.state("spectator"),
                    "head-snap yaw=" + input.value("original-delta-yaw") + " pitch=" + input.value("original-delta-pitch"));
        }
        if (!input.hasAll("source-horizontal", "source-previous-horizontal", "source-sprinting", "source-walk-speed", "source-speed-amplifier", "click-delay-ms")) return;
        double horizontal = input.value("source-horizontal");
        double acceleration = Math.abs(horizontal - input.value("source-previous-horizontal"));
        boolean shared = acceleration < .0025 && input.value("click-delay-ms") < 500 && input.state("target-player") && input.state("legacy-client");
        if (input.state("source-killaura-b-known")) {
            sink.sampleAbove("killaura-b", shared && input.value("source-sprinting") != 0
                    && horizontal > movementThreshold(input, .21f) && !input.state("source-killaura-b-exempt"), "sprinting-horizontal=" + horizontal);
        }
        if (input.state("source-killaura-l-known") && input.hasAll("max-no-damage-ticks", "target-max-no-damage-ticks")) {
            sink.sampleAbove("killaura-l", shared && input.value("source-sprinting") == 0
                    && horizontal > movementThreshold(input, .275f) && input.value("max-no-damage-ticks") >= 18
                    && input.value("target-max-no-damage-ticks") >= 18 && !input.state("source-killaura-l-exempt"), "walking-horizontal=" + horizontal);
        }
    }

    private float movementThreshold(Observation input, float base) {
        float speed = (float) input.value("source-speed-amplifier");
        float candidate = base + speed * .0675f + ((float) input.value("source-walk-speed") - .2f) * 3.5f;
        return candidate < base ? base + speed * .0675f : base;
    }

    private void criticals(Observation input, FindingSink sink) {
        if (!input.has("attack-movement-age") || input.value("attack-movement-age") >= 2 || !input.has("source-position-y")) return;
        double modulo = input.value("source-position-y") % .015625;
        if (input.state("source-criticals-a-known") && input.has("source-delta-y")) {
            double dy = Math.abs(input.value("source-delta-y"));
            if (Math.abs(dy - 7.504558659832128E-4) >= .001) {
                boolean suspicious = dy < .001 && dy > 0 && modulo < 1E-5 || dy == .0625;
                sink.sampleAbove("criticals-a", suspicious && !input.state("source-criticals-a-exempt"), "vertical-delta=" + dy + " grid-remainder=" + modulo);
            }
        }
        if (input.state("source-criticals-b-known")) sink.sampleAbove("criticals-b", modulo > 0 && modulo < 1E-5
                && !input.state("source-criticals-b-exempt"), "grid-remainder=" + modulo);
    }

    private void bow(Observation input, FindingSink sink) {
        if (!input.has("sequence-fastbow-interval-ms") || !marked(input, "source-fastbow-exempt-known")) return;
        double elapsed = input.value("sequence-fastbow-interval-ms");
        sink.sampleAbove("fastbow-a", elapsed < 750 && !input.state("sequence-fast") && !marked(input, "source-fastbow-exempt"), "full-strength-shot-interval-ms=" + elapsed);
    }

    private boolean marked(Observation input, String key) { return input.state(key) || input.has(key) && input.value(key) == 1; }

    private void reach(Observation input, FindingSink sink) {
        if (ready(input, "hitbox-b") && input.hasAll("source-hitbox-b-angle", "source-hitbox-b-range", "source-hitbox-b-max-angle", "source-hitbox-b-modern-minimum")) {
            double angle = input.value("source-hitbox-b-angle"), range = input.value("source-hitbox-b-range");
            double maximum = input.value("source-hitbox-b-max-angle"), modern = input.value("source-hitbox-b-modern-minimum");
            if (Double.isFinite(angle) && angle >= 0 && Double.isFinite(range)
                    && Double.isFinite(maximum) && maximum >= 0 && maximum <= 180 && (modern == 0 || modern == 1)) {
                double limit = maximum + (range < 1 ? .45 : 0);
                sink.sampleAbove("hitbox-b", angle > limit && range > (modern == 1 ? .3 : .8) && range < 10
                        && !input.state("source-hitbox-b-exempt"), "current-horizontal-angle-degrees=" + angle + " historical-range=" + range);
            }
        }
        if (ready(input, "reach-a") && input.has("source-reach-a-distance")) {
            double distance = input.value("source-reach-a-distance");
            if (distance == -1) sink.decay("reach-a", .025, "no-matching-target-history");
            else if (input.has("source-reach-a-limit")) sink.sampleAbove("reach-a", distance > input.value("source-reach-a-limit")
                    && distance < 8 && !input.state("source-reach-a-exempt"), "historical-horizontal-reach=" + distance);
        }
        if (ready(input, "reach-b") && input.hasAll("source-reach-b-distance", "source-reach-b-limit")) {
            double distance = input.value("source-reach-b-distance");
            sink.sampleAbove("reach-b", distance > input.value("source-reach-b-limit") && distance < 6
                    && !input.state("source-reach-b-exempt"), "current-horizontal-reach=" + distance);
        }
        if (ready(input, "hitbox-a") && input.has("source-hitbox-a-angle")) {
            double angle = input.value("source-hitbox-a-angle");
            if (angle == -1) sink.decay("hitbox-a", .025, "no-matching-target-history");
            else if (input.hasAll("source-hitbox-a-limit", "source-target-horizontal-distance")) {
                double distance = input.value("source-target-horizontal-distance");
                sink.sampleAbove("hitbox-a", angle > input.value("source-hitbox-a-limit") && distance > .25 && distance < 10
                        && !input.state("source-hitbox-a-exempt"), "historical-horizontal-angle-radians=" + angle);
            }
        }
    }

    private boolean ready(Observation input, String id) {
        return input.state("sequence-" + id + "-ready") && input.state("source-" + id + "-known") && !input.state("source-" + id + "-early-return");
    }

    private void velocity(Observation input, DetectionMemory memory, FindingSink sink) {
        if (input.state("source-velocity-a-known") && input.state("position-packet") && !input.state("source-velocity-a-early-return")
                && input.hasAll("source-velocity-y", "source-velocity-vertical-age", "source-delta-y", "source-jump-motion")) {
            double age = input.value("source-velocity-vertical-age");
            double expected = input.value("source-velocity-y");
            double actual = input.value("source-delta-y");
            if (age < 3 && expected > .01 && Math.abs(actual - input.value("source-jump-motion")) >= 1E-6) {
                if (age == 1) { memory.set("combat:velocity:first-ratio", actual / expected); memory.set("combat:velocity:ratio-phase", 1); }
                if (age == 2) { memory.set("combat:velocity:second-ratio", actual / expected); memory.set("combat:velocity:ratio-phase", 2); }
                if (memory.get("combat:velocity:ratio-phase", -1) == 2) {
                    double threshold = .999 - (input.state("source-velocity-horizontal-collision") ? .1 : 0) - (input.state("source-netherite-armor") ? .1 : 0);
                    double first = memory.get("combat:velocity:first-ratio", -1);
                    double second = memory.get("combat:velocity:second-ratio", -1);
                    sink.sampleAbove("velocity-a", first < threshold && second < threshold && expected > 0
                            && !input.state("source-velocity-a-exempt"), "vertical-response-ratios=" + first + "," + second);
                    memory.set("combat:velocity:first-ratio", -1); memory.set("combat:velocity:second-ratio", -1); memory.set("combat:velocity:ratio-phase", -1);
                }
            }
        }
        if (input.state("source-velocity-b-known") && !input.state("source-velocity-b-early-return")
                && input.hasAll("source-velocity-x", "source-velocity-z", "source-horizontal", "source-delta-y")) {
            double x = input.value("source-velocity-x"), z = input.value("source-velocity-z");
            double expected = Math.sqrt(x * x + z * z);
            if (expected >= .01 && input.value("source-delta-y") != .41999998688697815) {
                double ratio = input.value("source-horizontal") / expected;
                sink.sampleAbove("velocity-b", ratio < .21 && !input.state("source-velocity-b-exempt"), "horizontal-response-ratio=" + ratio);
            }
        }
        if (input.state("source-velocity-d-known") && !input.state("source-velocity-d-early-return") && !input.state("source-velocity-d-exempt")
                && input.hasAll("source-velocity-d-minimum-error", "source-velocity-d-threshold")) {
            double error = input.value("source-velocity-d-minimum-error");
            if (error <= 10) sink.sampleAbove("velocity-d", error > input.value("source-velocity-d-threshold"), "minimum-horizontal-error-squared=" + error);
        }
    }
}
