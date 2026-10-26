package io.github.lino9999.linoac.api;

import org.bukkit.event.HandlerList;
import java.util.UUID;

public final class LinoFreezeEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final boolean frozen;
    private final String actor;

    public LinoFreezeEvent(UUID playerId, String playerName, boolean frozen, String actor) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.frozen = frozen;
        this.actor = actor;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public boolean isFrozen() { return frozen; }
    public String getActor() { return actor; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
