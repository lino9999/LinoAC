package io.github.lino9999.linoac.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ProxyRelay implements AutoCloseable {
    private final Plugin plugin;
    private final BooleanSupplier enabled;
    private final Supplier<String> channel;
    private final Supplier<String> alertSeparator;
    private String registeredChannel;

    public ProxyRelay(Plugin plugin, BooleanSupplier enabled, Supplier<String> channel, Supplier<String> alertSeparator) {
        this.plugin = Objects.requireNonNull(plugin);
        this.enabled = Objects.requireNonNull(enabled);
        this.channel = Objects.requireNonNull(channel);
        this.alertSeparator = Objects.requireNonNull(alertSeparator);
    }

    public void configure() {
        primaryThread();
        String requested = Objects.requireNonNull(channel.get(), "proxy channel");
        if (requested.length() > Messenger.MAX_CHANNEL_SIZE || !requested.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")
                || requested.startsWith("minecraft:")) throw new IllegalArgumentException("Invalid proxy channel.");
        if (requested.equals(registeredChannel)) return;
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, requested);
        String previous = registeredChannel;
        registeredChannel = requested;
        if (previous != null) messenger.unregisterOutgoingPluginChannel(plugin, previous);
    }

    public boolean alert(Player carrier, ProxyPacketCodec.AlertFields fields) {
        primaryThread();
        return enabled.getAsBoolean() && send(carrier, "alert", ProxyPacketCodec.alertPayload(fields, alertSeparator.get()));
    }

    public boolean automaticCommand(Player carrier, String command) {
        primaryThread();
        return enabled.getAsBoolean() && send(carrier, "punishment", command);
    }

    public boolean automaticCommand(Player carrier, String command, ProxyPacketCodec.PunishmentFields fields) {
        primaryThread();
        return enabled.getAsBoolean() && send(carrier, "punishment",
                ProxyPacketCodec.punishmentPayload(command, fields, alertSeparator.get()));
    }

    public boolean manualCommand(Player carrier, String command) {
        primaryThread();
        return send(carrier, "punishment", command);
    }

    private boolean send(Player carrier, String operation, String payload) {
        if (carrier == null || !carrier.isOnline() || !plugin.isEnabled()) return false;
        byte[] bytes;
        try { bytes = ProxyPacketCodec.encode(operation, payload, Messenger.MAX_MESSAGE_SIZE); }
        catch (IllegalArgumentException failure) { plugin.getLogger().warning(failure.getMessage()); return false; }
        configure();
        carrier.sendPluginMessage(plugin, registeredChannel, bytes);
        return true;
    }

    private void primaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Proxy relay requires the server main thread.");
    }

    @Override
    public void close() {
        primaryThread();
        if (registeredChannel == null) return;
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, registeredChannel);
        registeredChannel = null;
    }
}
