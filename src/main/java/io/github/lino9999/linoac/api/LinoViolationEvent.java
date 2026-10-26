package io.github.lino9999.linoac.api;

import io.github.lino9999.linoac.check.CheckType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class LinoViolationEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final CheckType check;
    private final String detail;
    private final int violations;
    private boolean cancelled;

    public LinoViolationEvent(Player player, CheckType check, String detail, int violations) {
        this.player = player;
        this.check = check;
        this.detail = detail;
        this.violations = violations;
    }

    public Player getPlayer() { return player; }
    public CheckType getCheck() { return check; }
    public String getDetail() { return detail; }
    public int getViolations() { return violations; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean value) { cancelled = value; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
