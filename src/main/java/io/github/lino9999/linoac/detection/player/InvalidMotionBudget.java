package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.Observation;
import java.util.OptionalDouble;

public final class InvalidMotionBudget {
    private static final String[] HORIZONTAL_INPUTS = {"protocol-ground-run", "source-ceiling-position-age", "source-ice-position-age", "source-slime-below-position-age", "source-trapdoor-position-age", "source-bed-position-age", "source-fence-position-age", "source-soul-speed-position-age", "source-body-slime-position-age", "source-body-slab-position-age", "source-nearby-living-position-age", "source-stairs-contact", "source-slab-contact", "protocol-solid-center", "source-placed-ice-movement-age", "source-ravager-damage-movement-age", "source-crystal-damage-movement-age", "protocol-movement-speed", "protocol-speed-amplifier", "protocol-last-speed-amplifier", "source-inactive-reset-position-age", "protocol-invalid-j-entity-fix", "protocol-server-at-least-1-21-2", "source-velocity-position-age", "source-maximum-horizontal-velocity", "source-position-dy"};

    private InvalidMotionBudget() {}

    public static double vertical(boolean pendingExplosion, double explosionSumY, double lastPositiveExplosionY, int explosionPositionAge) {
        double maximum = 3.9210000038146973;
        if (pendingExplosion) maximum += .1 + explosionSumY;
        else if (explosionPositionAge < 40) maximum += lastPositiveExplosionY * Math.pow(.9800000190734863, explosionPositionAge);
        return maximum;
    }

    public static OptionalDouble legacyVertical(Observation o) {
        if (!o.hasAll("source-golem-damage-movement-age", "source-hoglin-damage-movement-age", "source-powder-snow-contact", "protocol-jump-amplifier")
                || !ProtocolExemptions.known(o, "FIREBALL", "BOAT", "SKULL", "FENCE")) return OptionalDouble.empty();
        return OptionalDouble.of(legacyVertical(new LegacyJump(integer(o, "source-golem-damage-movement-age"), exempt(o, "FIREBALL"), exempt(o, "BOAT"), exempt(o, "SKULL"),
                o.value("source-powder-snow-contact") == 1, integer(o, "source-hoglin-damage-movement-age"), integer(o, "protocol-jump-amplifier"), exempt(o, "FENCE"))));
    }

    public static double legacyVertical(LegacyJump jump) {
        double maximum = 1.25;
        if (jump.golemAge < 30) maximum += .4000000059604645;
        if (jump.fireball) maximum += .5;
        if (jump.boat) maximum += .125;
        if (jump.skull) maximum += .1;
        if (jump.powderSnow) maximum += .3;
        if (jump.hoglinAge < 20) maximum += .15000000596046448;
        if (jump.jumpAmplifier > 0) maximum += (double) (jump.jumpAmplifier * .125f);
        if (jump.fence) maximum += .25;
        return maximum;
    }

    public static OptionalDouble horizontal(Observation o) {
        if (!o.hasAll(HORIZONTAL_INPUTS) || !ProtocolExemptions.known(o, "FARMLAND", "CANCELLED_PLACE", "DEPTH_STRIDER", "SLAB", "STAIRS", "PISTON", "SNOW")) return OptionalDouble.empty();
        Terrain terrain = new Terrain(integer(o, "protocol-ground-run"), integer(o, "source-ceiling-position-age"), integer(o, "source-ice-position-age"),
                integer(o, "source-slime-below-position-age"), integer(o, "source-trapdoor-position-age"), integer(o, "source-bed-position-age"),
                integer(o, "source-fence-position-age"), integer(o, "source-soul-speed-position-age"), integer(o, "source-body-slime-position-age"),
                integer(o, "source-nearby-living-position-age"), o.value("source-stairs-contact") == 1, o.value("source-slab-contact") == 1, o.value("protocol-solid-center") == 1, integer(o, "source-body-slab-position-age"), integer(o, "source-inactive-reset-position-age"));
        Events events = new Events(integer(o, "source-placed-ice-movement-age"), integer(o, "source-ravager-damage-movement-age"), integer(o, "source-crystal-damage-movement-age"));
        Effects effects = new Effects(o.value("protocol-movement-speed"), integer(o, "protocol-speed-amplifier"), o.value("protocol-invalid-j-entity-fix") == 1, o.value("protocol-server-at-least-1-21-2") == 1, integer(o, "protocol-last-speed-amplifier"));
        Velocity velocity = new Velocity(integer(o, "source-velocity-position-age"), o.value("source-maximum-horizontal-velocity"));
        Allowances allowances = new Allowances(exempt(o, "FARMLAND"), exempt(o, "CANCELLED_PLACE"), exempt(o, "DEPTH_STRIDER"), exempt(o, "SLAB"), exempt(o, "STAIRS"), exempt(o, "PISTON"), exempt(o, "SNOW"));
        return OptionalDouble.of(horizontal(terrain, events, effects, velocity, allowances, o.value("source-position-dy")));
    }

    public static double horizontal(Terrain t, Events events, Effects effects, Velocity velocity, Allowances a, double signedDy) {
        double maximum = .2975 * (effects.movementSpeed / .1);
        boolean ceiling = t.ceilingAge < 45, ice = t.iceAge < 60, bed = t.bedAge < 30;
        boolean slime = t.slimeThreeBelowAge < 30, trapdoor = t.trapdoorAge < 40, fence = t.fenceAge < 20;
        switch (t.groundRun) {
            case 0 -> {
                maximum += .03;
                if (ice) maximum += .08;
                if (t.stairs) maximum += .35;
                if (ceiling && ice) maximum += .35;
                if (slime) maximum += .05;
                if (trapdoor && ice) maximum += .6;
                if (t.slabs) maximum += .04;
                if (t.iceAge == 0 && t.slabs) maximum += .1;
                if (trapdoor) maximum += .1;
                if (fence) maximum += .1;
                if (signedDy == .41999998688697815) maximum += .03;
            }
            case 1 -> {
                maximum += .025;
                if (ice) maximum += .1;
                if (t.stairs) maximum += .35;
                if (ceiling && ice) maximum += .45;
                if (slime) maximum += .05;
                if (trapdoor && ice) maximum += .55;
                if (t.slabs) maximum += .15;
                if (trapdoor) maximum += .1;
                if (fence) maximum += .1;
            }
            case 2 -> {
                maximum += .134;
                if (ceiling && !t.solidCenter) maximum += .06694;
                if (ice) maximum += .08;
                if (bed) maximum += .04;
                if (t.stairs) maximum += .35;
                if (ceiling && ice) maximum += .4;
                if (trapdoor && ice) maximum += .55;
                if (t.slabs) maximum += .15;
                if (trapdoor) maximum += .1;
                if (t.iceAge == 0 && t.slabs) maximum += .005;
                if (fence) maximum += .1;
            }
            case 3 -> {
                maximum += .0693;
                if (ceiling && !t.solidCenter) maximum += .036;
                if (ice) maximum += .045;
                if (t.stairs) maximum += .35;
                if (trapdoor && ice) maximum += .35;
                if (ceiling && ice) maximum += .35;
                if (t.slabs) maximum += .04;
                if (bed) maximum += .04;
                if (fence) maximum += .1;
                if (trapdoor) maximum += .1;
            }
            case 4 -> {
                maximum += .035;
                if (ceiling && !t.solidCenter) maximum += .0211;
                if (ice) maximum += .03;
                if (t.stairs) maximum += .35;
                if (trapdoor) maximum += .1;
                if (trapdoor && ice) maximum += .35;
                if (ceiling && ice) maximum += .35;
                if (t.slabs) maximum += .04;
            }
            case 5 -> {
                maximum += .0205;
                if (ceiling && !t.solidCenter) maximum += .0231;
                if (ice) maximum += .03;
                if (t.stairs) maximum += .35;
                if (trapdoor && ice) maximum += .35;
                if (t.slabs) maximum += .04;
            }
            case 6 -> {
                maximum += .009;
                if (ceiling && !t.solidCenter) maximum += .0027;
                if (ice) maximum += .02;
                if (trapdoor && ice) maximum += .35;
                if (t.slabs) maximum += .04;
            }
            case 7 -> { maximum += .005; if (ice) maximum += .02; if (trapdoor && ice) maximum += .1; }
            case 8 -> { maximum += .003; if (ice) maximum += .02; if (t.iceAge == 0 && t.slabs) maximum += .02; }
            case 9 -> { maximum += .0025; if (ice) maximum += .02; }
            default -> { }
        }
        if (t.iceAge == 0 && t.groundRun >= 3) maximum += .09;
        if (a.farmland) maximum += .05;
        if (t.iceAge < 30 && t.groundRun >= 5 && t.groundRun <= 25 && ceiling) maximum += .15;
        if (a.cancelledPlace) maximum += .08;
        if (t.iceAge < 30 && t.groundRun > 8) maximum += .15;
        if (a.depthStrider) maximum += .1;
        if (effects.modernServer && t.soulSpeedAge < 30 && effects.speedAmplifier > 3) maximum += 1.25;
        if (effects.modernServer && t.soulSpeedAge < 30) maximum += .5;
        if (ice && slime) maximum += .35;
        if (slime && ceiling) maximum += .1;
        if (a.slab) maximum += .3;
        if (a.stairs) maximum += .5;
        int age = velocity.positionAge;
        if (age >= 0 && age <= 7) maximum += velocity.maximumHorizontal + .05;
        else if (age >= 8 && age <= 15) maximum += velocity.maximumHorizontal * .75;
        else if (age >= 16 && age <= 20) maximum += velocity.maximumHorizontal * .6;
        else if (age >= 21 && age <= 25) maximum += velocity.maximumHorizontal * .5;
        else if (age >= 26 && age <= 30) maximum += velocity.maximumHorizontal * .35;
        else if (age >= 31 && age <= 35) maximum += velocity.maximumHorizontal * .25;
        else if (age >= 36 && age <= 40) maximum += velocity.maximumHorizontal * .1;
        if (t.bodySlimeAge < 15) maximum += .7;
        if (effects.entityCollisionFix && t.nearbyLivingAge < 20) maximum += .1;
        if (events.placedIceAge < 35) maximum += .2;
        if (a.piston) maximum += .325;
        if (t.bodySlabAge < 5) maximum += .025;
        if (a.snow) maximum += .05;
        if (events.ravagerDamageAge < 30) maximum += 2;
        if (events.crystalDamageAge < 5) maximum += .65;
        if (events.crystalDamageAge > 5 && events.crystalDamageAge < 25) maximum += .3;
        if (t.inactiveResetAge < 10) maximum += effects.lastSpeedAmplifier * .075;
        return maximum;
    }

    private static int integer(Observation observation, String key) { return (int) observation.value(key); }
    private static boolean exempt(Observation observation, String name) { return ProtocolExemptions.any(observation, name); }

    public record Terrain(int groundRun, int ceilingAge, int iceAge, int slimeThreeBelowAge, int trapdoorAge, int bedAge,
                          int fenceAge, int soulSpeedAge, int bodySlimeAge, int nearbyLivingAge, boolean stairs, boolean slabs,
                          boolean solidCenter, int bodySlabAge, int inactiveResetAge) {}
    public record Events(int placedIceAge, int ravagerDamageAge, int crystalDamageAge) {}
    public record Effects(double movementSpeed, int speedAmplifier, boolean entityCollisionFix, boolean modernServer, int lastSpeedAmplifier) {}
    public record Velocity(int positionAge, double maximumHorizontal) {}
    public record Allowances(boolean farmland, boolean cancelledPlace, boolean depthStrider, boolean slab, boolean stairs, boolean piston, boolean snow) {}
    public record LegacyJump(int golemAge, boolean fireball, boolean boat, boolean skull, boolean powderSnow, int hoglinAge, int jumpAmplifier, boolean fence) {}
}
