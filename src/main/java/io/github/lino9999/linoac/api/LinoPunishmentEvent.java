package io.github.lino9999.linoac.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.UUID;

public final class LinoPunishmentEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final String reason;
    private final List<String> commands;
    private boolean cancelled;

    public LinoPunishmentEvent(UUID playerId, String playerName, String reason, List<String> commands) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.reason = reason;
        this.commands = List.copyOf(commands);
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getReason() { return reason; }
    public List<String> getCommands() { return commands; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean value) { cancelled = value; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
