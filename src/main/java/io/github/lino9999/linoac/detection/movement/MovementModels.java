package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;
import java.util.OptionalDouble;

final class MovementModels {
    static final double[] FALL_LIMITS = {
            -0.5, -0.07850000262260437, -0.15530000627040863, -0.2305999994277954,
            -0.3043999969959259, -0.376800000667572, -0.44749999046325684, -0.5170999765396118,
            -0.5860000252723694, -0.6520000100135803, -0.7171000242233276, -0.7811999917030334,
            -0.843999981880188, -0.9054999947547913, -0.9657999873161316, -1.024899959564209,
            -1.082800030708313, -1.1395000219345093, -1.195099949836731, -1.2496000528335571,
            -1.3029999732971191, -1.3559999465942383, -1.406999945640564, -1.4570000171661377,
            -1.5069999694824219, -1.5549999475479126
    };
    private static final double[] JUMP = {
            0, 0.41999998688697815, 0.33319999363422426, 0.24813599859094637,
            0.164773281826065, 0.08307781780646906, -0.07840000152587834, -0.15523200451659847,
            -0.23052736891296632
    };
    private static final double[] LIQUID = {
            0.05999999821185753, 0.051999998867515274, 0.06159999881982969, 0.06927999889612124,
            0.07542399904870933, 0.08033919924402255, 0.08427135945886732, 0.0874170876776148,
            0.08993367029011523, 0.0519469373041872, -0.05647059355944606, 0.03812980539822064,
            -0.035014067535591664, -0.04453032983624894, 0.019999999105927202, -0.07159953051526458,
            0.020820931761605266, 0.0010261658043049238, -0.023717291273619878, -0.010724939925282229
    };
    private static final String[] JUMP_NUMBERS = {"protocol-jump-strength", "protocol-previous-jump-strength", "source-sulfur-position-age",
                "source-jump-support-position-age", "source-jump-event-movement-age", "protocol-jump-amplifier", "source-unassigned-effect-level",
                "source-jump-surface-position-age", "source-jump-change-movement-age", "source-explosion-sum-y", "source-last-positive-explosion-y",
                "source-explosion-position-age", "source-retained-velocity-y", "source-velocity-position-age"};
    private static final String[] JUMP_BITS = {"source-daylight-detector-contact", "source-lantern-contact", "source-chain-contact", "source-cake-contact",
                "source-campfire-contact", "source-snow-contact", "source-amethyst-contact", "source-heavy-core-contact", "source-flower-pot-contact",
                "source-bed-surface-contact", "source-powder-snow-contact", "source-stairs-contact", "source-client-ground",
                "source-explosion-pending", "source-velocity-pending", "source-unassigned-effect-active"};
    private MovementModels() {}

    static boolean liquidMotion(double dy) {
        if (Math.abs(dy) <= 0) return false;
        for (double candidate : LIQUID) if (Math.abs(dy - candidate) < .01) return true;
        return false;
    }


    static OptionalDouble jumpBudget(Observation input) {

        for (String key : JUMP_NUMBERS) if (!input.has(key) || !Double.isFinite(input.value(key))) return OptionalDouble.empty();

        for (String key : JUMP_BITS) if (MovementExemptions.bit(input, key) == null) return OptionalDouble.empty();
        if (!ProtocolExemptions.known(input, "SKULL", "SEA_PICKLE")) return OptionalDouble.empty();
        double maximum = input.value("protocol-jump-strength");
        if (input.value("source-sulfur-position-age") < 50) maximum += 1.25;
        if (active(input, "source-daylight-detector-contact")) maximum += .1;
        if (input.value("source-jump-support-position-age") < 10) maximum += 1;
        if (active(input, "source-lantern-contact")) maximum += .2;
        if (active(input, "source-chain-contact")) maximum += .2;
        if (ProtocolExemptions.any(input, "SKULL")) maximum += .20000000298023224;
        if (active(input, "source-cake-contact")) maximum += .1;
        if (active(input, "source-campfire-contact")) maximum += .20000000298023224;
        if (active(input, "source-snow-contact")) maximum += .10000000149011612;
        if (active(input, "source-amethyst-contact")) maximum += .25;
        if (active(input, "source-heavy-core-contact")) maximum += .1;
        if (active(input, "source-flower-pot-contact")) maximum += .1;
        if (active(input, "source-bed-surface-contact")) maximum += .1;
        if (ProtocolExemptions.any(input, "SEA_PICKLE")) maximum += .1;
        if (active(input, "source-powder-snow-contact")) maximum += .5;
        if (input.value("source-jump-event-movement-age") < 20) maximum += .15000000596046448;
        if (input.value("protocol-jump-amplifier") > 0) maximum += (double) ((float) input.value("protocol-jump-amplifier") * .125f);
        if (active(input, "source-unassigned-effect-active") && input.value("source-unassigned-effect-level") > 0) maximum += (double) ((float) input.value("source-unassigned-effect-level") * .125f);
        if (input.value("source-jump-surface-position-age") < 5) maximum += .125;
        if (input.value("source-jump-change-movement-age") < 20) maximum += Math.max(input.value("protocol-jump-strength"), input.value("protocol-previous-jump-strength"));
        if (active(input, "source-explosion-pending")) maximum += .25 + Math.abs(input.value("source-explosion-sum-y"));
        else if (input.value("source-explosion-position-age") < 40 && !active(input, "source-client-ground")) {
            maximum += Math.abs(input.value("source-last-positive-explosion-y") + .25) * Math.pow(.9800000190734863, input.value("source-explosion-position-age"));
        }
        if (active(input, "source-velocity-pending")) maximum += .1 + Math.abs(input.value("source-retained-velocity-y"));
        else if (input.value("source-velocity-position-age") < 40 && !active(input, "source-client-ground")) {
            maximum += Math.abs(input.value("source-retained-velocity-y")) * Math.pow(.9800000190734863, input.value("source-velocity-position-age"));
        }
        if (active(input, "source-stairs-contact")) maximum += .1;
        return Double.isFinite(maximum) ? OptionalDouble.of(maximum < .4 ? .41999998688697815 : maximum) : OptionalDouble.empty();
    }
    private static boolean active(Observation input, String key) { return input.value(key) == 1; }

    static double jump(int age, int level) { return JUMP[age] + (double) ((float) level * .1f); }
    static double drag(double base, double modifier) { return Math.max(0, Math.min(1, 1 - (1 - base) * modifier)); }
    static double vertical(double previous, double gravity, double drag, boolean matched, boolean legacy) {
        if (matched) return 0;
        double predicted = (previous - gravity) * drag;
        return Math.abs(predicted) < (legacy ? .005 : .003) ? 0 : predicted;
    }
}
