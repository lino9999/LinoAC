package io.github.lino9999.linoac.api;

import org.bukkit.event.HandlerList;
import java.util.UUID;

public final class LinoClientBrandEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final String previousBrand;
    private final String brand;

    public LinoClientBrandEvent(UUID playerId, String playerName, String previousBrand, String brand) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.previousBrand = previousBrand;
        this.brand = brand;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getPreviousBrand() { return previousBrand; }
    public String getBrand() { return brand; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
