package io.github.lino9999.linoac.api;

import io.github.lino9999.linoac.check.CheckType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class LinoAlertEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final CheckType check;
    private final int violations;
    private final String detail;
    private boolean cancelled;

    public LinoAlertEvent(Player player, CheckType check, int violations, String detail) {
        this.player = java.util.Objects.requireNonNull(player);
        this.check = java.util.Objects.requireNonNull(check);
        this.violations = violations;
        this.detail = java.util.Objects.requireNonNull(detail);
    }

    public Player getPlayer() { return player; }
    public CheckType getCheck() { return check; }
    public int getViolations() { return violations; }
    public String getDetail() { return detail; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean value) { cancelled = value; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
