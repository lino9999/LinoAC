package io.github.lino9999.linoac.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class LinoWaveEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final int attempted;
    private final int completed;
    private final long durationMillis;

    public LinoWaveEndEvent(int attempted, int completed, long durationMillis) {
        this.attempted = attempted;
        this.completed = completed;
        this.durationMillis = durationMillis;
    }

    public int getAttempted() { return attempted; }
    public int getCompleted() { return completed; }
    public long getDurationMillis() { return durationMillis; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
