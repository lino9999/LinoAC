package io.github.lino9999.linoac.api;

import io.github.lino9999.linoac.check.CheckType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public final class LinoPostViolationEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID playerId;
    private final String playerName;
    private final CheckType check;
    private final int violations;
    private final String detail;

    public LinoPostViolationEvent(UUID playerId, String playerName, CheckType check, int violations, String detail) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.check = check;
        this.violations = violations;
        this.detail = detail;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public CheckType getCheck() { return check; }
    public int getViolations() { return violations; }
    public String getDetail() { return detail; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
