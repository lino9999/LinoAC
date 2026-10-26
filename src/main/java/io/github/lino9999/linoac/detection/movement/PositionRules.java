package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

final class PositionRules {
    void evaluate(Observation input, DetectionMemory memory, FindingSink sink) {
        if (input.kind() != Observation.Kind.MOVE || !input.state("position-packet")) return;
        motion(input, sink);
        step(input, memory, sink);
        speed(input, sink);
        jump(input, sink);
        flight(input, sink);
    }

    private void motion(Observation o, FindingSink sink) {
        if (finite(o, "source-delta-y")) {
            double dy = o.value("source-delta-y");
            if (outer(o, false)) {
                double difference = Math.abs(Math.abs(dy) - .41999998688697815);
                strict(o, sink, "motion-c", Math.abs(dy) > .1 && difference > 0 && difference < 1e-5,
                        "absolute-jump-difference=" + difference);
            }
            if (outer(o, true) && finite(o, "source-previous-position-dy")) {
                double previous = o.value("source-previous-position-dy");
                if (Math.abs(dy - .11760000228882461) > .001 && finite(o, "source-climb-position-age")) {
                    strict(o, sink, "motion-a", dy > 0 && dy == previous && o.value("source-climb-position-age") >= 20,
                            "repeated-positive-motion=" + dy, "source-heavy-core-contact", "source-flower-pot-contact");
                }
                if (finite(o, "source-chorus-fruit-movement-age")) {
                    strict(o, sink, "motion-b", dy > 0 && dy == -previous && o.value("source-chorus-fruit-movement-age") >= 50,
                            "inverted-positive-motion=" + dy, "source-lectern-contact", "source-repeater-contact");
                }
            }
        }
        if (!finite(o, "source-horizontal", "protocol-slowness", "walk-speed", "protocol-speed-amplifier")) return;
        if (o.value("protocol-slowness") > 0 || o.value("walk-speed") > .25 || o.value("protocol-speed-amplifier") > 3) return;
        double horizontal = o.value("source-horizontal");
        double remainder = .1 % (horizontal % .1);
        strict(o, sink, "motion-e", horizontal > .11 && remainder < 1e-8, "horizontal-remainder=" + remainder);
    }

    private void step(Observation o, DetectionMemory memory, FindingSink sink) {
        if (outer(o, false) && finite(o, "source-delta-y") && bitKnown(o, "source-client-ground") && bitKnown(o, "client-at-least-1-21-2")) {
            boolean modern = o.value("client-at-least-1-21-2") == 1;
            if (!modern || finite(o, "protocol-step-height")) {
                double limit = modern ? o.value("protocol-step-height") : .6000000238418579;
                var exclusions = MovementExemptions.evaluate("step-a", o);
                if (o.value("source-client-ground") == 1 && o.value("source-delta-y") > limit && exclusions.known() && !exclusions.active()) {
                    sink.direct("step-a", "ground-claimed-rise=" + o.value("source-delta-y") + " limit=" + limit);
                }
            }
        }
        if (!outer(o, true) || !finite(o, "source-position-count", "protocol-air-drag", "source-last-break-distance")) return;
        if (o.value("source-position-count") < 30 || o.value("protocol-air-drag") != 1) return;
        double distance = o.value("source-last-break-distance");
        if (distance > 0 && distance < 1.25) return;
        if (!bitKnown(o, "source-cached-solid-footprint")) return;
        double count = o.value("source-cached-solid-footprint") == 1 ? Math.min(Integer.MAX_VALUE, memory.get("movement.supported-position-run") + 1) : 0;
        memory.set("movement.supported-position-run", count);
        var exclusions = MovementExemptions.including("step-c", o, "source-movement-step-c-private-exempt");
        if (!exclusions.known() || exclusions.active() || !finite(o, "source-client-air-movement-age", "source-delta-y")) return;
        int age = (int) o.value("source-client-air-movement-age");
        if (age < 0 || age >= MovementModels.FALL_LIMITS.length) return;
        boolean suspicious = o.value("source-delta-y") < MovementModels.FALL_LIMITS[age] && (age != 0 || count > 5);
        sink.sampleAbove("step-c", suspicious, "client-air-descent=" + o.value("source-delta-y") + " age=" + age);
    }

    private void speed(Observation o, FindingSink sink) {
        if (!outer(o, true) || !finite(o, "source-client-air-movement-age", "source-horizontal", "source-previous-horizontal", "protocol-air-drag")
                || !bitKnown(o, "protocol-server-at-least-26-1")) return;
        if (o.value("source-client-air-movement-age") <= 2 || o.value("source-horizontal") <= .25) return;
        var exclusions = MovementExemptions.including("speed-a", o, "source-movement-speed-a-private-exempt");
        if (!exclusions.known() || exclusions.active()) return;
        double predicted = o.value("source-previous-horizontal") * drag(o, .9100000262260437) + .026;
        double error = o.value("source-horizontal") - predicted;
        sink.sampleAbove("speed-a", error > 1e-5, "air-horizontal-residual=" + error);
    }
    private void jump(Observation o, FindingSink sink) {
        if (!outer(o, false) || !finite(o, "source-delta-y", "source-client-air-movement-age", "protocol-jump-amplifier")) return;
        double dy = o.value("source-delta-y"), air = o.value("source-client-air-movement-age");
        if (trajectoryReady(o, dy, air)) {
            double expected = MovementModels.jump((int) air, (int) o.value("protocol-jump-amplifier"));
            double error = Math.abs(dy - expected);
            if (finite(o, "protocol-jump-strength", "protocol-gravity", "protocol-bounciness", "protocol-air-drag",
                    "source-gravity-change-movement-age", "source-jump-change-movement-age")
                    && o.value("protocol-jump-strength") == .42 && o.value("protocol-gravity") == .08
                    && o.value("protocol-bounciness") <= 0 && o.value("protocol-air-drag") == 1
                    && o.value("source-gravity-change-movement-age") >= 20 && o.value("source-jump-change-movement-age") >= 20) {
                strictOrDirect(o, sink, "jump-a", dy > 0 && error > 1e-10, error > .5, "jump-trajectory-error=" + error);
            }
            if (dy == 0 && error > 1e-10 && error >= 10) {
                var exclusions = MovementExemptions.including("jump-c", o, "source-movement-jump-c-private-exempt");
                if (exclusions.known() && !exclusions.active()) sink.accumulate("jump-c", "zero-trajectory-error=" + error);
                else if (exclusions.active()) sink.sampleAbove("jump-c", false, "zero-trajectory-error=" + error);
            } else strictOrDirect(o, sink, "jump-c", dy == 0 && error > 1e-10, error > .5, "zero-trajectory-error=" + error);
        }
        if (!bitKnown(o, "source-movement-jump-b-early-exempt") || o.value("source-movement-jump-b-early-exempt") == 1) return;
        var budget = MovementModels.jumpBudget(o);
        if (budget.isEmpty()) return;
        double limit = budget.getAsDouble();
        strictOrDirect(o, sink, "jump-b", air > 0 && dy > limit, dy - limit > 1, "instantaneous-jump-rise=" + dy + " limit=" + limit);
    }

    private boolean trajectoryReady(Observation o, double dy, double air) {
        if (air < 1 || air > 8 || air != Math.rint(air) || (air == 6 || air == 7) && dy < .033) return false;
        if (!bitKnown(o, "source-jump-disabled") || o.value("source-jump-disabled") == 1
                || !finite(o, "source-bed-surface-position-age", "source-join-age-ms")) return false;
        if (o.value("source-bed-surface-position-age") < 30 || liquidPreserves(o)) return false;
        if (o.value("source-join-age-ms") >= 30000) return true;
        if (!finite(o, "source-join-x", "source-join-y", "source-join-z", "source-position-x", "source-position-y", "source-position-z")) return false;
        double x = o.value("source-position-x") - o.value("source-join-x");
        double y = o.value("source-position-y") - o.value("source-join-y");
        double z = o.value("source-position-z") - o.value("source-join-z");
        return Math.sqrt(x * x + y * y + z * z) >= 4;
    }

    private void strictOrDirect(Observation o, FindingSink sink, String id, boolean suspicious, boolean immediate, String detail) {
        if (!suspicious) { sink.sampleAbove(id, false, detail); return; }
        var exemptions = MovementExemptions.including(id, o, "source-movement-" + id + "-private-exempt");
        if (!exemptions.known()) return;
        if (exemptions.active()) sink.sampleAbove(id, false, detail);
        else sink.sampleAboveOrDirect(id, immediate, detail);
    }
    private void flight(Observation o, FindingSink sink) {
        if (!outer(o, true) || !finite(o, "source-delta-y", "source-previous-position-dy", "protocol-gravity")) return;
        double dy = o.value("source-delta-y"), gravity = o.value("protocol-gravity");
        double error = Math.abs(dy - (o.value("source-previous-position-dy") - gravity) * .9800000190734863);
        if (!finite(o, "source-sulfur-position-age")) return;
        boolean special = o.value("source-sulfur-position-age") < 50 && dy >= -1.25 && dy <= 1.25;
        if (finite(o, "source-cached-air-position-age", "protocol-jump-amplifier", "source-unassigned-effect-level")
                && bitKnown(o, "source-velocity-pending") && bitKnown(o, "source-unassigned-effect-active") && !ghostPreserves(o, "flight-a")) {
            double air = o.value("source-cached-air-position-age");
            Boolean fast = ProtocolExemptions.value(o, "FAST"), piston = ProtocolExemptions.value(o, "PISTON");
            if (o.value("source-velocity-pending") == 0 && Math.abs(dy - .11760000228882461) > .001
                    && dy + .09800000190734881 > .001 && (air >= 30 || Boolean.FALSE.equals(piston))) {
                double minimum = 6 + o.value("protocol-jump-amplifier") + (o.value("source-unassigned-effect-active") == 1 ? o.value("source-unassigned-effect-level") : 0);
                strict(o, sink, "flight-a", air > minimum && air > 6 && error > 1e-8 && !special,
                        "vertical-residual=" + error, "source-movement-flight-a-private-exempt");
            } else if (o.value("source-velocity-pending") == 0 && Math.abs(dy - .11760000228882461) <= .001
                    && Boolean.FALSE.equals(fast) && dy + .09800000190734881 > .001 && (air >= 30 || Boolean.FALSE.equals(piston))) {
                double minimum = 6 + o.value("protocol-jump-amplifier") + (o.value("source-unassigned-effect-active") == 1 ? o.value("source-unassigned-effect-level") : 0);
                strict(o, sink, "flight-a", air > minimum && air > 6 && error > 1e-8 && !special,
                        "vertical-residual=" + error, "source-movement-flight-a-private-exempt");
            }
        }
        if (finite(o, "source-client-air-movement-age", "protocol-jump-amplifier", "source-unassigned-effect-level", "source-gravity-change-movement-age")
                && o.value("protocol-jump-amplifier") <= 50 && o.value("source-unassigned-effect-level") <= 50
                && Math.abs(dy - .11760000228882461) > .001 && dy + .09800000190734881 > .005
                && o.value("source-gravity-change-movement-age") >= 20 && !liquidPreserves(o)) {
            strict(o, sink, "flight-b", o.value("source-client-air-movement-age") > 7 + o.value("protocol-jump-amplifier") && error > 1e-8 && !special,
                    "client-air-vertical-residual=" + error, "source-movement-flight-b-private-exempt");
        }
        if (gravity == .08 && finite(o, "source-cached-air-position-age")) {
            double air = o.value("source-cached-air-position-age");
            var placement = ProtocolExemptions.evaluate(o, "BLOCK_PLACE", "CANCELLED_PLACE");
            if (placement.known() && finite(o, "source-position-y", "source-dragon-damage-movement-age") && !lowAltitudePreserves(o)
                    && o.value("source-dragon-damage-movement-age") >= 200) {
                if (!ghostPreserves(o, "flight-c") && Math.abs(dy) - .08307781780646728 >= .001) {
                    strict(o, sink, "flight-c", air > 7 + (placement.active() ? 15 : 0) && dy > 0 && !special,
                            "cached-air-ascension=" + dy, "source-movement-flight-c-private-exempt");
                }
                if (!ghostPreserves(o, "flight-d") && finite(o, "protocol-jump-strength", "source-gravity-change-movement-age")
                        && o.value("protocol-jump-strength") <= 2 && o.value("source-gravity-change-movement-age") >= 20
                        && dy + .09800000190734881 > .001) {
                    strict(o, sink, "flight-d", air > 18 + (placement.active() ? 30 : 0) && dy > -.5 && !special,
                            "cached-air-shallow-fall=" + dy, "source-movement-flight-d-private-exempt");
                }
            }
            if (!ghostPreserves(o, "flight-e") && bitKnown(o, "source-cached-air")) {
                strict(o, sink, "flight-e", air > 3 && Math.abs(dy) < .5 && Math.abs(dy - o.value("source-previous-position-dy")) < .05
                                && o.value("source-cached-air") == 1 && !special,
                        "stable-air-motion=" + dy, "source-movement-flight-e-private-exempt");
            }
        }
        flightHistory(o, sink, special);
    }

    private void flightHistory(Observation o, FindingSink sink, boolean special) {
        if (!finite(o, "protocol-jump-amplifier", "source-unassigned-effect-level", "source-server-position-movement-age", "source-gravity-change-movement-age")) return;
        if (o.value("protocol-jump-amplifier") > 50 || o.value("source-unassigned-effect-level") > 50
                || o.value("source-server-position-movement-age") == 1 || o.value("source-gravity-change-movement-age") < 20) return;
        if (!bitKnown(o, "source-gliding-active") || o.value("source-gliding-active") == 1
                || !bitKnown(o, "protocol-server-at-least-26-1")) return;
        if (!finite(o, "source-history-current-dy", "source-history-previous-dy", "protocol-air-drag", "source-client-air-movement-age",
                "source-block-place-movement-age", "source-flight-surface-position-age", "source-flight-event-movement-age", "source-flight-direct-position-age")) return;
        String[] bits = {"source-history-current-ground", "source-history-previous-ground", "source-history-current-matched", "source-history-previous-matched",
                "source-velocity-pending", "source-explosion-pending", "client-at-least-1-9"};
        for (String key : bits) if (!bitKnown(o, key)) return;
        boolean legacy = o.value("client-at-least-1-9") == 0;
        double dy = o.value("source-history-current-dy"), air = o.value("source-client-air-movement-age");
        double predicted = MovementModels.vertical(o.value("source-history-previous-dy"), o.value("protocol-gravity"),
                drag(o, .9800000190734863), o.value("source-history-previous-matched") == 1, legacy);
        double tolerance = 1e-8 + (air == 7 ? .03 : 0);
        if (Math.abs(dy - .40444491418477924) <= .025 && o.value("source-block-place-movement-age") < 20) tolerance += .75;
        if (o.value("source-flight-surface-position-age") < 10) tolerance += .6;
        if (o.value("source-flight-event-movement-age") < 30) tolerance += .1;
        if (legacy) {
            if (!bitKnown(o, "source-previous-position-packet") || !bitKnown(o, "source-previous-previous-position-packet")
                    || !finite(o, "source-pitch", "source-horizontal")) return;
            if (o.value("source-previous-position-packet") == 0 || o.value("source-previous-previous-position-packet") == 0) tolerance += .03;
            if (o.value("source-block-place-movement-age") < 20 && o.value("source-pitch") < 45) tolerance += .02;
            if (air == 6 && o.value("source-horizontal") < .1) tolerance += .1;
            if (air == 7 && o.value("source-horizontal") < .1) tolerance += .2;
            if (air == 7 && Math.abs(dy - (0 - o.value("protocol-gravity")) * .9800000190734863) < 5e-4) tolerance += .2;
        }
        double error = Math.abs(dy - predicted);
        boolean numeric = o.value("source-history-current-matched") == 0 && o.value("source-history-current-ground") == 0
                && o.value("source-history-previous-ground") == 0 && error > tolerance && o.value("source-velocity-pending") == 0
                && o.value("source-explosion-pending") == 0 && !special;
        var exclusions = MovementExemptions.including("flight-f", o, "source-movement-flight-f-private-exempt");
        if (!numeric || exclusions.known() && exclusions.active()) sink.sampleAbove("flight-f", false, "history-vertical-error=" + error);
        else if (exclusions.known()) sink.sampleAboveOrDirect("flight-f", error > 1 && o.value("source-flight-direct-position-age") > 20,
                "history-vertical-error=" + error + " tolerance=" + tolerance);
    }

    private double drag(Observation input, double base) {
        return input.value("protocol-server-at-least-26-1") == 1 ? MovementModels.drag(base, input.value("protocol-air-drag")) : base;
    }

    private boolean ghostPreserves(Observation o, String id) {
        String key = "movement-ignore-ghost-blocks-" + id;
        if (!bitKnown(o, key)) return true;
        if (o.value(key) == 0) return false;
        if (!bitKnown(o, "source-client-ground") || !bitKnown(o, "source-cached-air") || !finite(o, "source-position-y")) return true;
        return o.value("source-client-ground") == 1 && o.value("source-cached-air") == 1 && o.value("source-position-y") % .015625 == 0;
    }
    private boolean lowAltitudePreserves(Observation o) {
        if (!bitKnown(o, "movement-ignore-low-altitude")) return true;
        double y = o.value("source-position-y");
        return o.value("movement-ignore-low-altitude") == 1 && y > -5 && y < 5;
    }
    private boolean liquidPreserves(Observation o) {
        if (!bitKnown(o, "movement-ignore-liquid-motion")) return true;
        if (o.value("movement-ignore-liquid-motion") == 0) return false;
        if (!finite(o, "source-horizontal")) return true;
        if (o.value("source-horizontal") >= .15) return false;
        if (!bitKnown(o, "source-cached-solid-footprint")) return true;
        return o.value("source-cached-solid-footprint") == 1 && MovementModels.liquidMotion(o.value("source-delta-y"));
    }

    private void strict(Observation o, FindingSink sink, String id, boolean suspicious, String detail, String... extra) {
        if (!suspicious) { sink.sampleAbove(id, false, detail); return; }
        var result = MovementExemptions.including(id, o, extra);
        if (result.known()) sink.sampleAbove(id, !result.active(), detail);
    }

    private boolean outer(Observation input, boolean origin) {
        if (!bitKnown(input, "source-server-position-match") || input.value("source-server-position-match") != 0) return false;
        return !origin || bitKnown(input, "source-origin-skip") && input.value("source-origin-skip") == 0;
    }
    private boolean bitKnown(Observation input, String key) { return MovementExemptions.bit(input, key) != null; }
    private boolean finite(Observation input, String... keys) {
        for (String key : keys) if (!input.has(key) || !Double.isFinite(input.value(key))) return false;
        return true;
    }
}
