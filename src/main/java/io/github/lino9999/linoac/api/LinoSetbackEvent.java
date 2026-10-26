package io.github.lino9999.linoac.api;

import io.github.lino9999.linoac.check.CheckType;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public final class LinoSetbackEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final CheckType check;
    private final Location from;
    private final Location to;

    public LinoSetbackEvent(UUID playerId, String playerName, CheckType check, Location from, Location to) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.check = check;
        this.from = from.clone();
        this.to = to.clone();
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public CheckType getCheck() { return check; }
    public Location getFrom() { return from.clone(); }
    public Location getTo() { return to.clone(); }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
