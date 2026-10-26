package io.github.lino9999.linoac.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class PendingPunishmentActions<T> {
    private final Map<UUID, Entry<T>> entries = new HashMap<>();
    private final Consumer<T> cancel;

    public PendingPunishmentActions(Consumer<T> cancel) { this.cancel = Objects.requireNonNull(cancel); }

    public Ticket reserve(UUID player) {
        Objects.requireNonNull(player);
        if (entries.containsKey(player)) return null;
        Ticket ticket = new Ticket(player);
        entries.put(player, new Entry<>(ticket, null));
        return ticket;
    }

    public void attach(Ticket ticket, T action) {
        Objects.requireNonNull(action);
        Entry<T> entry = entries.get(ticket.player);
        if (entry == null || entry.ticket != ticket) { cancel.accept(action); return; }
        if (entry.action != null) throw new IllegalStateException("A punishment action is already attached.");
        entries.put(ticket.player, new Entry<>(ticket, action));
    }

    public boolean release(Ticket ticket) {
        Entry<T> entry = entries.get(ticket.player);
        if (entry == null || entry.ticket != ticket) return false;
        entries.remove(ticket.player);
        return true;
    }

    public void cancel(UUID player) {
        Entry<T> entry = entries.remove(player);
        if (entry != null && entry.action != null) cancel.accept(entry.action);
    }

    public void cancelAll() {
        var pending = java.util.List.copyOf(entries.values());
        entries.clear();
        pending.forEach(entry -> { if (entry.action != null) cancel.accept(entry.action); });
    }

    public static final class Ticket {
        private final UUID player;
        private Ticket(UUID player) { this.player = player; }
    }

    private record Entry<T>(Ticket ticket, T action) {}
}
