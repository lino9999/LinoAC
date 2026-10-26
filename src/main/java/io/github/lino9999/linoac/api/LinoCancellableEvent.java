package io.github.lino9999.linoac.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class LinoCancellableEvent extends Event implements Cancellable {
    private boolean cancelled;

    public final boolean isCancelled() { return cancelled; }
    public final void setCancelled(boolean value) { cancelled = value; }
}
