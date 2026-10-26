package io.github.lino9999.linoac.api;

import io.github.lino9999.linoac.check.CheckType;
import org.bukkit.event.HandlerList;

public final class LinoCheckToggleEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CheckType check;
    private final boolean enabled;
    private final String actor;

    public LinoCheckToggleEvent(CheckType check, boolean enabled, String actor) {
        this.check = check;
        this.enabled = enabled;
        this.actor = actor;
    }

    public CheckType getCheck() { return check; }
    public boolean isEnabled() { return enabled; }
    public String getActor() { return actor; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
