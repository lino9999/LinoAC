package io.github.lino9999.linoac.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public final class LinoPlayerRegisterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;

    public LinoPlayerRegisterEvent(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
