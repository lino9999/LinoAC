package io.github.lino9999.linoac.detection.collector;

public final class SourceAbilityState {
    private double glidingRevision = -1;
    private boolean gliding;
    private boolean glidingKnown = true;
    private int glidingAge = 100;
    private boolean slowKnown = true, levitationKnown = true, riptideKnown = true;
    private int slowAge = 100, levitationAge = 100, riptideAge = 200, dolphinAge = 100, elytraAge = 100;
    private boolean swimmingKnown = true;
    private int swimmingAge = 100;

    public void metadata(Boolean active, double revision) {
        if (active == null || !Double.isFinite(revision)) { glidingKnown = false; return; }
        if (revision != glidingRevision) { gliding = active; glidingRevision = revision; }
        glidingKnown = true;
    }

    public void elytraAction(boolean equipped) { if (equipped) { gliding = true; glidingKnown = true; } }
    public void flying() { if (glidingKnown) glidingAge = gliding ? 0 : increment(glidingAge); }

    public void position(Boolean slowFalling, Boolean levitation, Boolean riptide, boolean dolphin, boolean previousChestElytra) {
        position(slowFalling, levitation, riptide, dolphin, previousChestElytra, false);
    }
    public void position(Boolean slowFalling, Boolean levitation, Boolean riptide, boolean dolphin, boolean previousChestElytra, Boolean swimming) {
        slowKnown = slowFalling != null; levitationKnown = levitation != null; riptideKnown = riptide != null;
        if (slowKnown) slowAge = slowFalling ? 0 : increment(slowAge);
        if (levitationKnown) levitationAge = levitation ? 0 : increment(levitationAge);
        if (riptideKnown) riptideAge = riptide ? 0 : increment(riptideAge);
        dolphinAge = dolphin ? 0 : increment(dolphinAge);
        elytraAge = previousChestElytra ? 0 : increment(elytraAge);
        swimmingKnown = swimming != null;
        if (swimmingKnown) swimmingAge = swimming ? 0 : increment(swimmingAge);
    }

    public Snapshot snapshot() { return new Snapshot(glidingKnown, glidingAge, slowKnown, slowAge, levitationKnown, levitationAge,
            riptideKnown, riptideAge, dolphinAge, elytraAge, swimmingKnown, swimmingAge); }
    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
    public record Snapshot(boolean glidingKnown, int glidingAge, boolean slowKnown, int slowAge, boolean levitationKnown,
                           int levitationAge, boolean riptideKnown, int riptideAge, int dolphinAge, int elytraAge, boolean swimmingKnown, int swimmingAge) {}
}
