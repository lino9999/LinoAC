package io.github.lino9999.linoac.api;

import org.bukkit.event.HandlerList;

public final class LinoWebhookPunishmentEvent extends LinoCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String message;

    public LinoWebhookPunishmentEvent(String message) { this.message = message; }

    public String getMessage() { return message; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
