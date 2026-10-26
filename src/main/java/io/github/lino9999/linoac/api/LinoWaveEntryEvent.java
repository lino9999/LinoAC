package io.github.lino9999.linoac.api;

import org.bukkit.event.HandlerList;
import java.util.List;
import java.util.UUID;

public final class LinoWaveEntryEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final List<String> commands;

    public LinoWaveEntryEvent(UUID playerId, String playerName, List<String> commands) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.commands = List.copyOf(commands);
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public List<String> getCommands() { return commands; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
