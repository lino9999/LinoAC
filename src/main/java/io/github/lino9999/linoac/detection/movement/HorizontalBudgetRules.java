package io.github.lino9999.linoac.detection.movement;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule.FindingSink;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

import java.util.Set;

final class HorizontalBudgetRules {
    static final Set<String> IDS = Set.of("elytra-n", "speed-b", "speed-c", "speed-d", "speed-e");
    private static final String[] GROUND_EXCLUSIONS = {"FLIGHT", "JOINED", "CREATIVE", "SHULKER", "SHULKER_BOX", "DOLPHINS_GRACE", "TELEPORT", "ILLEGAL_BLOCK", "CHUNK", "ELYTRA", "GLIDING", "RIPTIDE", "VEHICLE", "BOAT", "FISHING_ROD", "ENTITY_CRAM_FIX", "DRAGON_DAMAGE", "DEATH", "SLEEPING", "ENDER_PEARL", "FROZEN", "CHORUS_FRUIT", "SPECTATOR", "WORLD_CHANGE", "ANVIL", "CANCELLED_MOVE"};
    private static final String[] ALIGNED_EXCLUSIONS = {"FLIGHT", "JOINED", "CREATIVE", "SHULKER", "SHULKER_BOX", "DOLPHINS_GRACE", "TELEPORT", "ILLEGAL_BLOCK", "GLIDING", "CANCELLED_MOVE", "RIPTIDE", "VEHICLE", "BOAT", "FISHING_ROD", "ELYTRA", "CHUNK", "DEATH", "SLEEPING", "ENDER_PEARL", "FROZEN", "CHORUS_FRUIT", "ENTITY_CRAM_FIX", "SPECTATOR", "WORLD_CHANGE", "ANVIL", "DRAGON_DAMAGE"};
    private static final String[] EQUIPMENT_EXCLUSIONS = {"FLIGHT", "JOINED", "CREATIVE", "SHULKER", "SHULKER_BOX", "DOLPHINS_GRACE", "TELEPORT", "ILLEGAL_BLOCK", "CHUNK", "RIPTIDE", "VEHICLE", "BOAT", "FISHING_ROD", "ENTITY_CRAM_FIX", "DEATH", "SLEEPING", "ENDER_PEARL", "FROZEN", "CHORUS_FRUIT", "SPECTATOR", "WORLD_CHANGE", "ANVIL", "CANCELLED_MOVE"};
    private static final String[] AIR_EXCLUSIONS = {"JOINED", "ILLEGAL_BLOCK", "RIPTIDE", "SHULKER", "CHORUS_FRUIT", "FLIGHT", "CREATIVE", "VEHICLE", "SHULKER_BOX", "CHUNK", "DRAGON_DAMAGE", "FISHING_ROD", "DOLPHINS_GRACE", "ENDER_PEARL", "TELEPORT", "WORLD_CHANGE", "FROZEN", "DEATH", "SLEEPING", "BOAT", "SPECTATOR", "GLIDING", "ANVIL", "FULLY_STUCK", "PARTIALLY_STUCK", "CANCELLED_MOVE", "ENTITY_CRAM_FIX"};
    private static final String[] PREDICTION_EXCLUSIONS = {"FLIGHT", "TELEPORT", "SLEEPING", "GLIDING", "RIPTIDE", "WEB", "CREATIVE", "ENDER_PEARL", "VEHICLE", "WORLD_CHANGE", "CHUNK", "TELEPORT", "HONEY", "CHORUS_FRUIT"};
    private static final double[][] PHASE = {
            {.323, .076, .08, 0, .35, .35, .05, .6, .04, .1, .1, .1, .03},
            {.025, .0728, .1, 0, .35, .45, .05, .55, .15, 0, .1, .1, 0},
            {.134, .06694, .08, .04, .35, .4, 0, .55, .15, .005, .1, .1, 0},
            {.0693, .036, .045, .04, .35, .35, 0, .35, .04, 0, .1, .1, 0},
            {.035, .0211, .03, 0, .35, .35, 0, .35, .04, 0, .1, 0, 0},
            {.0205, .0231, .03, 0, .35, 0, 0, .35, .04, 0, 0, 0, 0},
            {.009, .0027, .02, 0, 0, 0, 0, .35, .04, 0, 0, 0, 0},
            {.005, 0, .02, 0, 0, 0, 0, .1, 0, 0, 0, 0, 0},
            {.003, 0, .02, 0, 0, 0, 0, 0, 0, .02, 0, 0, 0},
            {.0025, 0, .02, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };
    private static final int[][] PHASE_ORDER = {
            {1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12}, {1, 2, 4, 5, 6, 7, 8, 10, 11},
            {1, 2, 3, 4, 5, 7, 8, 10, 9, 11}, {1, 2, 4, 7, 5, 8, 3, 11, 10},
            {1, 2, 4, 10, 7, 5, 8}, {1, 2, 4, 7, 8}, {1, 2, 7, 8}, {2, 7}, {2, 9}, {2}
    };

    void evaluate(Observation input, DetectionMemory memory, FindingSink sink) {
        if (input.kind() != Observation.Kind.MOVE || !input.state("position-packet") || !MovementRuleInputs.outer(input, true, true)) return;
        if (MovementRuleInputs.finite(input, "protocol-jump-amplifier", "source-unassigned-effect-level")
                && input.value("protocol-jump-amplifier") <= 50 && input.value("source-unassigned-effect-level") <= 50) {
            phase(input, sink, PhaseKind.GROUND);
            phase(input, sink, PhaseKind.ALIGNED);
            phase(input, sink, PhaseKind.EQUIPMENT);
            airborne(input, sink);
        }
        prediction(input, sink);
    }

    private void phase(Observation input, FindingSink sink, PhaseKind mode) {
        View v = new View(input);
        double phase = v.n(mode == PhaseKind.ALIGNED ? "source-y-aligned-position-count" : "source-client-ground-movement-age");
        PhaseContacts contacts = PhaseContacts.read(v);
        double budget = phaseBase(v, phase, mode == PhaseKind.ALIGNED, contacts);
        budget = mode == PhaseKind.ALIGNED ? alignedAllowances(v, phase, budget, contacts) : sharedAllowances(v, phase, budget, contacts, mode == PhaseKind.EQUIPMENT);
        if (mode != PhaseKind.EQUIPMENT) {
            if (v.n("protocol-air-drag") < 1) budget = Math.max(budget, adaptive(v, v.b("source-previous-client-ground")));
            budget = Math.max(budget, .295);
        } else {
            budget = Math.max(budget, .295);
            if ((v.n("source-any-gliding-position-age") < 20 || v.n("source-flight-event-movement-age") < 20) && phase < 5) budget += v.n("source-retained-glide-horizontal");
            double effectAge = v.n("source-speed-absent-position-age");
            if (effectAge > 0 && effectAge < 10) budget += v.n("protocol-last-speed-amplifier") * .075;
            if (v.n("source-firework-position-age") < 20 && phase < 5) budget += 1.5;
        }
        String id = mode == PhaseKind.GROUND ? "speed-b" : mode == PhaseKind.ALIGNED ? "speed-d" : "elytra-n";
        double difference = v.n("source-horizontal") - budget;
        double threshold = mode == PhaseKind.EQUIPMENT ? .01 : v.n("source-" + id + "-min-difference");
        boolean equipment = mode != PhaseKind.EQUIPMENT || v.b("source-equipped-elytra");
        finish(v, sink, id, difference > threshold && phase >= 0 && equipment, difference,
                mode == PhaseKind.GROUND ? GROUND_EXCLUSIONS : mode == PhaseKind.ALIGNED ? ALIGNED_EXCLUSIONS : EQUIPMENT_EXCLUSIONS,
                mode == PhaseKind.ALIGNED ? new String[]{"SERVER_POSITION"} : new String[]{"SERVER_POSITION", "CHORUS_FRUIT"});
    }

    static double phaseBase(View v, double phase, boolean aligned, PhaseContacts c) {
        double result = .2975 * (v.n("protocol-movement-speed") / .1);
        if (phase >= 0 && phase <= 9 && phase == Math.rint(phase)) {
            int index = (int) phase;
            double[] row = PHASE[index];
            result += row[0];
            for (int term : PHASE_ORDER[index]) {
                boolean active = switch (term) {
                    case 1 -> c.ceiling && !c.shape;
                    case 2 -> c.ice;
                    case 3 -> c.bed;
                    case 4 -> c.contact;
                    case 5 -> c.ceiling && c.ice;
                    case 6 -> c.slime;
                    case 7 -> c.trapdoor && c.ice;
                    case 8 -> c.surface;
                    case 9 -> c.iceAge == 0 && c.surface;
                    case 10 -> c.trapdoor;
                    case 11 -> c.fence;
                    case 12 -> v.n("source-delta-y") == .41999998688697815;
                    default -> false;
                };
                if (active) result += aligned && term == 1 ? switch (index) {
                    case 4 -> .0411;
                    case 5 -> .0431;
                    case 6 -> .0527;
                    default -> row[term];
                } : aligned && index == 2 && term == 8 ? .05 : row[term];
            }
        }
        if (c.iceAge == 0 && phase > 2) result += .09;
        return result;
    }

    private double sharedAllowances(View v, double phase, double budget, PhaseContacts c, boolean equipment) {
        if (v.exempt("FARMLAND")) budget += .05;
        if (c.iceAge < 30 && phase >= 5 && phase <= 25 && c.ceiling) budget += .15;
        if (v.exempt("CANCELLED_PLACE")) budget += .08;
        if (c.iceAge < 30 && phase > 8) budget += .15;
        if (v.exempt("DEPTH_STRIDER")) budget += .1;
        if (equipment && v.b("protocol-server-at-least-1-16") && v.n("source-soul-speed-position-age") < 30 && v.n("source-speed-amplifier") > 3) budget += 1.25;
        if (v.b("protocol-server-at-least-1-16") && v.n("source-soul-speed-position-age") < 30) budget += .5;
        if (c.ice && c.slime) budget += .35;
        if (c.slime && c.ceiling) budget += .1;
        if (equipment) budget += equipmentCarry(v);
        if (v.n("source-body-slime-position-age") < 25) budget += 1.5;
        if (v.b("protocol-server-at-least-1-9") && v.b("source-entity-collision-allowance-enabled") && v.n("source-nearby-living-position-age") < 20) budget += .1;
        if (v.n("source-placed-ice-movement-age") < 35) budget += .2;
        if (v.exempt("PISTON")) budget += .325;
        if (v.n("source-body-slab-position-age") < 5) budget += .025;
        if (v.exempt("SNOW")) budget += .05;
        if (v.n("source-ravager-damage-movement-age") < 30) budget += 2;
        double abilityAge = v.n("source-crystal-damage-movement-age");
        if (abilityAge < 5) budget += 1;
        if (abilityAge > 5 && abilityAge < 25) budget += .75;
        if (!equipment && v.b("source-lantern-contact")) budget += .02;
        if (v.b("protocol-exempt-PRESSURE_PLATE")) budget += .08;
        if (v.b("source-door-contact") && c.ceilingAge < 10) budget += .1;
        if (!equipment) {
            budget += explosionCarry(v);
            if (c.ceilingAge < 10 && c.iceAge < 10) budget += .55;
            if (c.ceilingAge < 10 && (v.b("source-chain-contact") || v.b("protocol-exempt-CONDUIT") || v.b("source-lantern-contact"))) budget += .1;
            if (v.b("protocol-exempt-GLASS_PANE")) budget += .075;
            if (v.n("protocol-gravity") > .2) budget += .025;
            double speed = v.n("protocol-movement-speed");
            if (speed < .1 && phase <= 1) budget += .1 - speed + .01;
            if (v.n("protocol-slowness") > 0 && phase <= 1) budget += v.n("protocol-slowness") * .05;
            if (v.n("source-speed-absent-position-age") < 0) budget += v.n("protocol-last-speed-amplifier") * .075;
            budget += velocityCarry(v);
        }
        return budget;
    }

    private double alignedAllowances(View v, double phase, double budget, PhaseContacts c) {
        if (c.ceilingAge < 5) budget += .1;
        if (c.ceilingAge < 20 && phase < 15 && !c.shape) budget += .1;
        if (c.surface) budget += .1;
        if (c.iceAge < 30 && phase >= 5 && phase <= 25 && c.ceiling) budget += .15;
        if (v.exempt("CANCELLED_PLACE")) budget += .08;
        if (v.n("source-slab-contact-position-age") < 20) budget += .075;
        if (c.iceAge < 30 && phase > 8) budget += .15;
        if (v.n("protocol-gravity") > .2) budget += .025;
        if (v.exempt("DEPTH_STRIDER")) budget += .1;
        if (v.exempt("FARMLAND")) budget += .05;
        if (v.b("protocol-server-at-least-1-16") && v.n("source-soul-speed-position-age") < 30) budget += .5;
        if (c.ice && c.slime) budget += .35;
        if (c.slime && c.ceiling) budget += .1;
        budget += velocityCarry(v);
        if (v.n("source-ice-contact-position-age") < 40) budget += .125;
        if (v.b("source-trapdoor-contact")) budget += .125;
        if (v.n("source-body-slime-position-age") < 15) budget += .6;
        if (v.b("protocol-server-at-least-1-9") && v.b("source-entity-collision-allowance-enabled") && v.n("source-nearby-living-position-age") < 20) budget += .1;
        if (v.n("source-placed-ice-movement-age") < 40) budget += .2;
        if (v.exempt("PISTON")) budget += .325;
        if (v.exempt("CAKE")) budget += .1;
        if (v.exempt("STAIRS") || v.exempt("SLAB")) budget += .45;
        if (v.exempt("BED")) budget += .25;
        if (v.exempt("FENCE")) budget += .225;
        if (v.exempt("SNOW")) budget += .1;
        if (v.b("source-amethyst-contact")) budget += .05000000074505806;
        double dy = v.n("source-delta-y");
        if (v.b("source-amethyst-contact") && (dy == .4375 || dy == .5625)) budget += .25;
        if (v.exempt("SNOW") && dy == .5) budget += .15000000596046448;
        if (v.n("source-ravager-damage-movement-age") < 30) budget += 2;
        double abilityAge = v.n("source-crystal-damage-movement-age");
        if (abilityAge < 5) budget += 1;
        if (abilityAge > 5 && abilityAge < 25) budget += .5;
        if (v.b("protocol-exempt-PRESSURE_PLATE")) budget += .08;
        if (v.b("source-sculk-sensor-contact")) budget += .125;
        if (v.b("source-door-contact") && c.ceilingAge < 10) budget += .15;
        if (v.b("source-lantern-contact")) budget += .075;
        if (v.b("source-chain-contact")) budget += .05;
        if (v.b("source-chain-contact") && dy == .59375) budget += .2;
        budget += explosionCarry(v);
        if (c.ceilingAge < 10 && c.iceAge < 10) budget += .55;
        if (v.b("protocol-exempt-GLASS_PANE")) budget += .075;
        double speed = v.n("protocol-movement-speed");
        if (speed < .1 && phase <= 1) budget += .1 - speed + .01;
        if (v.n("protocol-slowness") > 0 && phase <= 1) budget += v.n("protocol-slowness") * .05;
        if (v.b("source-heavy-core-contact")) budget += .25;
        if (v.n("source-speed-absent-position-age") < 0) budget += v.n("protocol-last-speed-amplifier") * .075;
        return budget;
    }

    private void airborne(Observation input, FindingSink sink) {
        View v = new View(input);
        double phase = v.n("source-client-air-movement-age"), ceiling = v.n("source-ceiling-position-age"), ice = v.n("source-ice-position-age");
        double budget = .3625 * (v.n("protocol-movement-speed") / .1);
        if (ceiling < 30) budget += .1;
        if (v.exempt("CANCELLED_PLACE")) budget += .1;
        if (v.n("source-body-slime-position-age") < 15) budget += .6;
        if (phase == 2 && ceiling < 50 && v.n("source-delta-y") == -.07840000152587834) budget += .165;
        if (ice < 40) budget += .25;
        if (ceiling < 30 && ice < 40) budget += .3;
        if (v.exempt("STAIRS")) budget += .05;
        if (v.n("source-slime-below-position-age") < 30) budget += .1;
        if (v.b("protocol-server-at-least-1-16") && v.n("source-soul-speed-position-age") < 30) budget += .3;
        budget += velocityCarry(v);
        if (v.n("source-velocity-position-age") < 10) budget += .025;
        budget += explosionCarry(v);
        if (v.n("source-explosion-position-age") < 10) budget += .025;
        if (v.b("protocol-server-after-1-13") && v.n("source-honey-position-age") < 20) budget += .125;
        if (v.n("source-placed-ice-movement-age") < 40) budget += .2;
        if (v.exempt("FARMLAND")) budget += .025;
        if (v.b("protocol-server-at-least-1-9") && v.b("source-entity-collision-allowance-enabled") && v.n("source-nearby-living-position-age") < 20) budget += .1;
        if (v.exempt("PISTON")) budget += .325;
        if (v.exempt("SNOW")) budget += .05;
        if (v.n("source-fireball-damage-movement-age") < 30) budget += .75;
        double abilityAge = v.n("source-crystal-damage-movement-age");
        if (abilityAge < 5) budget += 1;
        if (abilityAge > 5 && abilityAge < 25) budget += .5;
        if (v.n("source-speed-absent-position-age") < 0) budget += v.n("protocol-last-speed-amplifier") * .075;
        if (v.n("source-any-gliding-position-age") < 30 || v.n("source-flight-event-movement-age") < 30) budget += v.n("source-retained-glide-horizontal") + .15;
        if (v.n("protocol-friction-modifier") < 1) budget = Math.max(budget, v.n("source-previous-horizontal") * drag(v, .91) + .046);
        if (v.n("protocol-air-drag") < 1) budget = Math.max(budget, adaptive(v, false));
        budget = Math.max(budget, .36);
        double difference = v.n("source-horizontal") - budget;
        finish(v, sink, "speed-c", difference > v.n("source-speed-c-min-difference") && phase > 1, difference, AIR_EXCLUSIONS, new String[]{"SERVER_POSITION_FAST"});
    }

    private void prediction(Observation input, FindingSink sink) {
        View v = new View(input);
        double speed = v.n("protocol-movement-speed"), friction = friction(v, .6), prediction = v.n("source-previous-horizontal") * drag(v, .9100000262260437), margin = .0001;
        if (v.n("source-liquid-contact-position-age") < 30) {
            margin += .1;
            if (v.n("source-dolphin-contact-position-age") < 30) margin += 1.25;
            if (v.n("source-dolphin-contact-position-age") < 20 && v.exempt("DEPTH_STRIDER")) margin += 1.5;
            if (v.n("source-explosion-position-age") < 20) margin += v.n("source-explosion-sum-horizontal") + .05;
            if (v.b("source-y-aligned")) margin += .25;
        }
        if (v.n("source-body-slime-position-age") < 10) prediction += .3499999940395355;
        if (v.b("source-previous-previous-client-ground")) prediction *= friction;
        if (v.b("source-velocity-pending")) prediction += v.n("source-maximum-horizontal-velocity") + .025;
        if (v.b("source-explosion-pending")) prediction += v.n("source-explosion-sum-horizontal") + .025;
        double piston = v.n("source-piston-position-age");
        if (piston < 20) prediction += .3;
        boolean ground = v.b("source-client-ground"), previousGround = v.b("source-previous-client-ground");
        if (previousGround) {
            if (!ground && v.n("source-delta-y") >= 0 || v.n("protocol-jump-amplifier") < 0) prediction += .20000000298023224;
            double acceleration = speed * 1.3;
            if (v.n("protocol-friction-modifier") == 1) prediction += acceleration * .16277136 / Math.pow(.546, 3);
            else if (friction <= .6) prediction += acceleration;
            else prediction += acceleration * .21600002 / Math.pow(friction, 3);
        } else prediction += .025999998673796654;
        if (v.n("source-flight-event-movement-age") < 20) prediction += v.n("source-retained-glide-horizontal");
        if (v.n("source-nearby-living-position-age") < 20) margin += .155;
        double iceContact = v.n("source-ice-contact-position-age");
        if (iceContact < 20 && v.n("source-trapdoor-position-age") < 20) margin += .41;
        if (iceContact < 15) margin += .31;
        if (v.n("source-slime-below-position-age") < 15) margin += .21;
        if (v.n("source-jump-support-position-age") < 15) margin += .31;
        boolean swift = v.b("source-swift-sneak");
        double sneakAge = v.n("source-block-place-movement-age");
        if (swift && sneakAge < 30) margin += .15;
        if (v.exempt("FARMLAND")) margin += .05;
        if (iceContact < 15 && v.n("source-ceiling-position-age") < 15) margin += .35;
        if (v.n("source-fishing-rod-movement-age") < 30) margin += .3;
        if (v.n("source-swimming-contact-position-age") < 20 && sneakAge < 20) margin += .15;
        if (v.n("source-contact-damage-movement-age") < 25 && v.n("source-ice-position-age") < 25) margin += .525;
        if (v.n("source-placed-soul-movement-age") < 30 && swift && v.n("source-soul-speed-position-age") < 30) margin += .15;
        double horizontal = v.n("source-horizontal"), difference = horizontal - prediction;
        boolean suspicious = difference > margin && horizontal > Math.max(.2625, speed * (ground && previousGround ? 2 : 1.125)) && piston >= 20;
        if (!v.known) return;
        MovementRuleInputs.strict(input, sink, "speed-e", suspicious, "horizontal-prediction-difference=" + difference + ", margin=" + margin, PREDICTION_EXCLUSIONS);
    }

    private static double equipmentCarry(View v) {
        double age = v.n("source-velocity-position-age"), impulse = v.n("source-maximum-horizontal-velocity");
        if (age >= 0 && age <= 7) return impulse + .05;
        if (age <= 15 && age >= 8) return impulse * .75;
        if (age <= 20 && age >= 16) return impulse * .6;
        if (age <= 25 && age >= 21) return impulse * .5;
        if (age <= 30 && age >= 26) return impulse * .35;
        if (age <= 35 && age >= 31) return impulse * .25;
        return age >= 36 && age <= 40 ? impulse * .1 : 0;
    }

    private static double velocityCarry(View v) { return v.n("source-maximum-horizontal-velocity") * Math.pow(.9150000214576721, v.n("source-velocity-position-age")); }
    private static double explosionCarry(View v) { return v.n("source-last-positive-explosion-horizontal") * Math.pow(.9150000214576721, v.n("source-explosion-position-age")); }
    private static double drag(View v, double base) { return v.b("protocol-server-at-least-26-1") ? MovementModels.drag(base, v.n("protocol-air-drag")) : base; }
    private static double friction(View v, double base) { return v.b("protocol-server-at-least-26-1") ? MovementModels.drag(base, v.n("protocol-friction-modifier")) : base; }
    private static double adaptive(View v, boolean ground) {
        double drag = drag(v, .91);
        if (drag <= .91) return 0;
        return v.n("source-previous-horizontal") * drag * (ground ? .6 : 1) + (ground ? v.n("protocol-movement-speed") * 1.3 : .026) + .02;
    }

    private static void finish(View v, FindingSink sink, String id, boolean suspicious, double difference, String[] exclusions, String[] immediateExclusions) {
        if (!v.known) return;
        String detail = "horizontal-budget-difference=" + difference;
        if (!suspicious) { sink.sampleAbove(id, false, detail); return; }
        var exempt = ProtocolExemptions.evaluate(v.input, exclusions);
        if (!exempt.known()) return;
        if (exempt.active()) { sink.sampleAbove(id, false, detail); return; }
        boolean immediate = difference > .5 && difference < 100;
        if (immediate) {
            var direct = ProtocolExemptions.evaluate(v.input, immediateExclusions);
            if (!direct.known()) return;
            immediate = !direct.active();
        }
        sink.sampleAboveOrDirect(id, immediate, detail);
    }

    private enum PhaseKind { GROUND, ALIGNED, EQUIPMENT }
    static final class View {
        private final Observation input;
        private boolean known = true;
        View(Observation input) { this.input = input; }
        double n(String key) {
            if (!input.has(key) || !Double.isFinite(input.value(key))) { known = false; return Double.NaN; }
            return input.value(key);
        }
        boolean b(String key) {
            double value = n(key);
            if (value != 0 && value != 1) known = false;
            return value == 1;
        }
        boolean exempt(String name) {
            Boolean value = ProtocolExemptions.value(input, name);
            if (value == null) known = false;
            return Boolean.TRUE.equals(value);
        }
    }

    record PhaseContacts(boolean shape, double ceilingAge, boolean ceiling, double iceAge, boolean ice, boolean bed, boolean contact, boolean slime, boolean trapdoor, boolean surface, boolean fence) {
        static PhaseContacts read(View v) {
            double ceiling = v.n("source-ceiling-position-age"), ice = v.n("source-ice-position-age");
            return new PhaseContacts(v.b("protocol-solid-center"), ceiling, ceiling < 45, ice, ice < 60, v.n("source-bed-position-age") < 30,
                    v.b("source-stairs-contact"), v.n("source-slime-below-position-age") < 30, v.n("source-trapdoor-position-age") < 40, v.b("source-slab-contact"), v.n("source-fence-position-age") < 20);
        }
    }
}
