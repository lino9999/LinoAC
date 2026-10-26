package io.github.lino9999.linoac.api;

import org.bukkit.event.HandlerList;
import java.util.UUID;

public final class LinoAlertToggleEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final boolean enabled;

    public LinoAlertToggleEvent(UUID playerId, String playerName, boolean enabled) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.enabled = enabled;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public boolean isEnabled() { return enabled; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
