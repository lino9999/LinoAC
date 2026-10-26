package io.github.lino9999.linoac.api;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public final class LinoGhostBlockEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final Location location;

    public LinoGhostBlockEvent(UUID playerId, String playerName, Location location) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.location = location.clone();
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public Location getLocation() { return location.clone(); }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
