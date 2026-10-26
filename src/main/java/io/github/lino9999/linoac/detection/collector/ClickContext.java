package io.github.lino9999.linoac.detection.collector;

public final class ClickContext {
    private int placementMoves = 100;
    private int breakMoves = 100;
    private int bottleMoves = 100;
    private int fireworkMoves = 200;
    private long lastMovementMillis;
    private long lastDiggingTick;
    private long lastBlockInteractionTick;
    private long lastDropTick;
    private long lastInventoryTick;
    private long lastCancelledBreakTick;
    private long lastItemFrameTick;
    private long lastProjectileTick;
    private long lastFishingTick;
    private long lastDigStartMillis;
    private long lastDigFinishMillis;
    private long lastDigCancelMillis;
    private long lastAnimalInteractionMillis;
    private long lastNoteBlockMillis;
    private double x;
    private double z;
    private double digX;
    private double digZ;
    private double dugBlockDistance;
    private boolean digging;
    private boolean fast;
    private final long[] clickIntervals = new long[20];
    private long lastClickMillis = -1;
    private long clickDelayMillis;
    private int clickIntervalCount;
    private int clickIntervalIndex;
    private double clicksPerSecond;

    public ClickContext(long createdMillis) {
        lastAnimalInteractionMillis = createdMillis;
        lastNoteBlockMillis = createdMillis;
    }

    public void movement(long millis, double currentX, double currentZ) {
        fast = millis - lastMovementMillis <= 5;
        lastMovementMillis = millis;
        placementMoves = increment(placementMoves);
        breakMoves = increment(breakMoves);
        bottleMoves = increment(bottleMoves);
        fireworkMoves = increment(fireworkMoves);
        if (Double.isFinite(currentX) && Double.isFinite(currentZ)) { x = currentX; z = currentZ; }
    }

    public void swing(long tick, boolean holdingFirework) {
        if (holdingFirework) fireworkMoves = 0;
        if (digging) lastDiggingTick = tick;
        if (digX != 0 && digZ != 0) {
            dugBlockDistance = Math.hypot(x - digX, z - digZ);
            if (dugBlockDistance > 12) { dugBlockDistance = 0; digX = 0; digZ = 0; }
        }
    }

    public Timing observeSwing(long tick, long millis, boolean creative, boolean holdingFirework) {
        boolean nonDigExempt = snapshot(tick, millis, creative).nonDigExempt();
        if (!nonDigExempt) {
            if (lastClickMillis > 0) {
                if (millis < lastClickMillis) {
                    clickIntervalCount = 0;
                    clickIntervalIndex = 0;
                    clickDelayMillis = 0;
                    clicksPerSecond = 0;
                } else {
                    clickDelayMillis = millis - lastClickMillis;
                    clickIntervals[clickIntervalIndex] = clickDelayMillis;
                    clickIntervalIndex = (clickIntervalIndex + 1) % clickIntervals.length;
                    clickIntervalCount = Math.min(clickIntervalCount + 1, clickIntervals.length);
                    if (clickIntervalCount == clickIntervals.length) {
                        double sum = 0;
                        for (int i = 0; i < clickIntervals.length; i++) sum += clickIntervals[(clickIntervalIndex + i) % clickIntervals.length];
                        clicksPerSecond = 20 / (sum / clickIntervals.length) * 50;
                    }
                }
            }
            lastClickMillis = millis;
        }
        swing(tick, holdingFirework);
        return new Timing(nonDigExempt, snapshot(tick, millis, creative).statisticalExempt(), clickDelayMillis,
                clicksPerSecond, clickIntervalCount == clickIntervals.length);
    }

    public void placement() { placementMoves = 0; }

    public void blockBreak(long tick, double blockX, double blockZ, boolean cancelled) {
        digging = false;
        breakMoves = 0;
        digX = blockX;
        digZ = blockZ;
        if (cancelled) lastCancelledBreakTick = tick;
    }

    public void blockInteraction(long tick, long millis, boolean rightClick, String itemType, String blockType) {
        if (rightClick && "GLASS_BOTTLE".equals(itemType)) bottleMoves = 0;
        if (blockType != null) {
            lastBlockInteractionTick = tick;
            if (rightClick && "NOTE_BLOCK".equals(blockType)) lastNoteBlockMillis = millis;
        }
    }

    public void digging(long tick, long millis, DigAction action, double blockX, double blockZ) {
        switch (action) {
            case START -> { digging = true; lastDigStartMillis = millis; }
            case FINISH -> { digging = false; lastDigFinishMillis = millis; digX = blockX; digZ = blockZ; }
            case CANCEL -> { digging = false; lastDigCancelMillis = millis; digX = blockX; digZ = blockZ; }
            case DROP -> lastDropTick = tick;
        }
    }

    public void attack() { digging = false; dugBlockDistance = 1000; }
    public void inventoryClick(long tick) { lastInventoryTick = tick; }
    public void itemFrameInteraction(long tick) { lastItemFrameTick = tick; }
    public void projectileLaunch(long tick) { lastProjectileTick = tick; }
    public void fishing(long tick) { lastFishingTick = tick; }
    public void fireworkCollision() { fireworkMoves = 0; }
    public long cancelledBreakAge(long tick) { return Math.max(0, tick - lastCancelledBreakTick); }

    public void entityInteraction(long millis, String entityType) {
        if ("VILLAGER".equals(entityType) || "COW".equals(entityType) || "CHICKEN".equals(entityType) || "WOLF".equals(entityType)) lastAnimalInteractionMillis = millis;
    }

    public Snapshot snapshot(long tick, long millis, boolean creative) {
        boolean nonDigExempt = placementMoves < 10 || digging || tick - lastDiggingTick < 20 || tick - lastBlockInteractionTick < 20
                || breakMoves < 25 || fast || tick - lastDropTick < 15 || tick - lastInventoryTick < 15
                || tick - lastCancelledBreakTick < 40 || creative || bottleMoves < 15
                || tick - lastItemFrameTick < 20 || tick - lastProjectileTick < 20;
        boolean statisticalExempt = nonDigExempt || millis - lastDigStartMillis < 110 || fireworkMoves < 20
                || millis - lastNoteBlockMillis < 3000 || millis - lastAnimalInteractionMillis < 250
                || millis - lastDigCancelMillis < 110 || millis - lastDigFinishMillis < 110
                || tick - lastFishingTick < 20 || dugBlockDistance > 0 && dugBlockDistance < 6.5;
        return new Snapshot(nonDigExempt, statisticalExempt, placementMoves, breakMoves, bottleMoves, fireworkMoves, fast, dugBlockDistance);
    }

    private int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }

    public enum DigAction { START, FINISH, CANCEL, DROP }

    public record Snapshot(boolean nonDigExempt, boolean statisticalExempt, int placementMoves, int breakMoves,
                           int bottleMoves, int fireworkMoves, boolean fast, double dugBlockDistance) {}

    public record Timing(boolean nonDigExempt, boolean statisticalExempt, long delayMs, double cps, boolean cpsReady) {}
}
