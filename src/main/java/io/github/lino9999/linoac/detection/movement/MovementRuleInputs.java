package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

final class MovementRuleInputs {
    private MovementRuleInputs() {}

    static boolean finite(Observation input, String... keys) {
        for (String key : keys) if (!input.has(key) || !Double.isFinite(input.value(key))) return false;
        return true;
    }

    static Boolean bit(Observation input, String key) {
        if (!input.has(key)) return null;
        return input.value(key) == 0 ? Boolean.FALSE : input.value(key) == 1 ? Boolean.TRUE : null;
    }

    static boolean outer(Observation input, boolean positionMatch, boolean origin) {
        return (!positionMatch || Boolean.FALSE.equals(bit(input, "source-server-position-match")))
                && (!origin || Boolean.FALSE.equals(bit(input, "source-origin-skip")));
    }

    static void strict(Observation input, FindingSink sink, String id, boolean suspicious, String detail, String... exclusions) {
        if (!suspicious) { sink.sampleAbove(id, false, detail); return; }
        var result = ProtocolExemptions.evaluate(input, exclusions);
        if (result.known()) sink.sampleAbove(id, !result.active(), detail);
    }

    static double drag(Observation input, double base) {
        return input.value("protocol-server-at-least-26-1") == 1 ? MovementModels.drag(base, input.value("protocol-air-drag")) : base;
    }

    static double angle(double dx, double dz, float yaw) {
        double radians = yaw * 3.1415927f / 180f;
        double x = -Math.sin(radians) * .5, z = Math.cos(radians) * .5;
        double denominator = Math.sqrt(dx * dx + dz * dz) * Math.sqrt(x * x + z * z);
        return (float) Math.acos(Math.max(-1, Math.min(1, (dx * x + dz * z) / denominator)));
    }
}
