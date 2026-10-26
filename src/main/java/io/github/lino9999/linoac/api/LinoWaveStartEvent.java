package io.github.lino9999.linoac.api;

import org.bukkit.event.HandlerList;
import java.util.Map;
import java.util.UUID;

public final class LinoWaveStartEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String actor;
    private final Map<UUID, String> entries;

    public LinoWaveStartEvent(String actor, Map<UUID, String> entries) {
        this.actor = actor;
        this.entries = Map.copyOf(entries);
    }

    public String getActor() { return actor; }
    public Map<UUID, String> getEntries() { return entries; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
