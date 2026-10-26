package io.github.lino9999.linoac.api;

import org.bukkit.event.HandlerList;
import java.util.Optional;
import java.util.UUID;

public final class LinoResetEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String actor;
    private final boolean automatic;

    public LinoResetEvent(UUID playerId, String actor, boolean automatic) {
        this.playerId = playerId;
        this.actor = actor;
        this.automatic = automatic;
    }

    public Optional<UUID> getPlayerId() { return Optional.ofNullable(playerId); }
    public boolean isGlobal() { return playerId == null; }
    public String getActor() { return actor; }
    public boolean isAutomatic() { return automatic; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
