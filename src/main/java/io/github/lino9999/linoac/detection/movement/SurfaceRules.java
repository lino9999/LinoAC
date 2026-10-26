package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

final class SurfaceRules {
    void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        if (o.kind() != Observation.Kind.MOVE || !o.state("position-packet")) return;
        levitation(o, sink);
        climbing(o, memory, sink);
        liquid(o, sink);
        slow(o, sink);
    }

    private void levitation(Observation o, FindingSink sink) {
        if (!finite(o, "protocol-levitation-amplifier", "source-delta-y", "source-position-y")
                || !known(o, "source-server-position-match", "source-client-ground", "source-cached-air")
                || active(o, "source-server-position-match") || o.value("protocol-levitation-amplifier") < 0
                || !Boolean.TRUE.equals(ProtocolExemptions.value(o, "SERVER_VERSION"))) return;
        boolean suspicious = o.value("protocol-levitation-amplifier") > 0
                && (active(o, "source-client-ground") || o.value("source-delta-y") < 0
                || !active(o, "source-cached-air") && aligned(o));
        strict(o, sink, "antilevitation-a", suspicious, true, "levitation-motion=" + o.value("source-delta-y"));
    }

    private void climbing(Observation o, DetectionMemory memory, FindingSink sink) {
        if (!finite(o, "protocol-gravity", "source-last-break-distance", "source-delta-y") || o.value("protocol-gravity") < .08) return;
        double broken = o.value("source-last-break-distance"), dy = o.value("source-delta-y");
        if (broken > 0 && broken < 1.25) return;
        if (finite(o, "source-velocity-position-age", "source-explosion-position-age")
                && o.value("source-velocity-position-age") >= 20 && o.value("source-explosion-position-age") >= 20
                && known(o, "source-climb-materials", "source-climb-ready") && active(o, "source-climb-materials")
                && finite(o, "source-climb-ready-position-count")) {
            strict(o, sink, "fastclimb-a", dy > .118 && o.value("source-climb-ready-position-count") > 3 && active(o, "source-climb-ready"),
                    false, "climb-rise=" + dy);
        }
        if (!finite(o, "source-wallclimb-gate-age", "source-position-x", "source-position-z")
                || o.value("source-wallclimb-gate-age") < 30 || Math.abs(dy - .20000004768371582) < .001) return;
        double count = memory.get("movement.wall-edge-position-count");
        boolean edge = band(o.value("source-position-x")) || band(o.value("source-position-z"));
        count = edge ? Math.min(Integer.MAX_VALUE, count + 1) : 0;
        if (known(o, "source-below-one-air") && !active(o, "source-below-one-air")) count = 0;
        memory.set("movement.wall-edge-position-count", count);
        if (!finite(o, "source-wall-air-count-a", "source-wall-air-count-b") || !known(o, "source-wallclimb-ready")) return;
        boolean suspicious = count > 5 && dy > .1 && o.value("source-wall-air-count-a") == o.value("source-wall-air-count-b")
                && active(o, "source-wallclimb-ready");
        strict(o, sink, "wallclimb-a", suspicious, false, "wall-edge-run=" + count + " rise=" + dy);
    }

    private void liquid(Observation o, FindingSink sink) {
        if (!known(o, "source-liquid-surface") || !finite(o, "source-position-y", "source-delta-y", "source-horizontal")) return;
        boolean surface = active(o, "source-liquid-surface");
        double dy = o.value("source-delta-y"), horizontal = o.value("source-horizontal");
        if (o.value("source-position-y") <= 250) {
            if (known(o, "source-client-ground")) {
                strict(o, sink, "jesus-a", surface && (active(o, "source-client-ground") || aligned(o)) && horizontal > .1,
                        true, "liquid-ground-motion=" + horizontal);
            }
            var b = SurfaceExemptions.evaluate("jesus-b", o, true);
            if (surface && b.known() && !b.active() && finite(o, "source-budget-jesus-b")) {
                sink.sampleAbove("jesus-b", Math.abs(dy) < .015 && horizontal > o.value("source-budget-jesus-b"),
                        "liquid-horizontal=" + horizontal + " limit=" + o.value("source-budget-jesus-b"));
            }
            strict(o, sink, "jesus-c", surface && Math.abs(dy) < .001, true, "liquid-stationary-height=" + dy);
            if (known(o, "source-central-feet-air")) {
                var d = SurfaceExemptions.evaluate("jesus-d", o, true);
                if (!surface || !active(o, "source-central-feet-air") || d.active()) sink.sampleAbove("jesus-d", false, "liquid-rise=" + dy);
                else if (d.known() && dy > 0) sink.sampleAbove("jesus-d", true, "liquid-rise=" + dy);
            }
        }
        var e = SurfaceExemptions.evaluate("jesus-e", o, true);
        if (!surface || e.active()) sink.sampleAbove("jesus-e", false, "liquid-speed=" + horizontal);
        else if (e.known() && finite(o, "source-budget-jesus-e") && horizontal > o.value("source-budget-jesus-e")) {
            sink.sampleAbove("jesus-e", true, "liquid-speed=" + horizontal + " limit=" + o.value("source-budget-jesus-e"));
        }
    }

    private void slow(Observation o, FindingSink sink) {
        if (!finite(o, "source-horizontal", "source-walk-speed", "source-speed-amplifier")) return;
        double horizontal = o.value("source-horizontal");
        if (finite(o, "protocol-movement-speed", "source-position-y") && known(o, "source-central-feet-soul-sand")) {
            double limit = (float) (movementBudget(o, .2f) + (float) o.value("source-speed-amplifier") * .0625f);
            boolean suspicious = active(o, "source-central-feet-soul-sand") && aligned(o) && horizontal > limit
                    && o.value("protocol-movement-speed") <= .10999999940395355;
            strict(o, sink, "noslow-b", suspicious, false, "soul-sand-horizontal=" + horizontal + " limit=" + limit);
        }
        if (known(o, "source-central-body-web")) {
            double limit = movementBudget(o, .09f);
            strict(o, sink, "noslow-c", active(o, "source-central-body-web") && horizontal > limit, false,
                    "web-horizontal=" + horizontal + " limit=" + limit);
        }
    }

    private void strict(Observation o, FindingSink sink, String id, boolean suspicious, boolean includePrivate, String detail) {
        if (!suspicious) { sink.sampleAbove(id, false, detail); return; }
        var result = SurfaceExemptions.evaluate(id, o, includePrivate);
        if (result.active()) sink.sampleAbove(id, false, detail);
        else if (result.known()) sink.sampleAbove(id, true, detail);
    }
    private static float movementBudget(Observation o, float base) {
        float speed = (float) o.value("source-speed-amplifier");
        float candidate = base + speed * .0675f + ((float) o.value("source-walk-speed") - .2f) * 3.5f;
        return candidate < base ? base + speed * .0675f : base;
    }
    private static boolean aligned(Observation o) { return o.value("source-position-y") % .015625 == 0; }
    private static boolean band(double coordinate) {
        double fraction = Math.abs(coordinate % 1);
        return fraction > .67 && fraction < .73 || fraction > .27 && fraction < .33;
    }
    private static boolean finite(Observation o, String... keys) {
        for (String key : keys) if (!o.has(key) || !Double.isFinite(o.value(key))) return false;
        return true;
    }
    private static boolean known(Observation o, String... keys) {
        for (String key : keys) if (!finite(o, key) || o.value(key) != 0 && o.value(key) != 1) return false;
        return true;
    }
    private static boolean active(Observation o, String key) { return o.value(key) == 1; }
}
