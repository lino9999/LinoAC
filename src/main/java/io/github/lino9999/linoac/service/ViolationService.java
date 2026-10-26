package io.github.lino9999.linoac.service;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.api.LinoViolationEvent;
import io.github.lino9999.linoac.check.CheckPolicy;
import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.player.PlayerData;
import io.github.lino9999.linoac.detection.FindingBuffer;
import io.github.lino9999.linoac.detection.FlagEligibility;
import io.github.lino9999.linoac.detection.AlertSelection;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ViolationService {
    private final LinoAC plugin;

    public ViolationService(LinoAC plugin) { this.plugin = plugin; }

    public boolean eligible(Player player, PlayerData data) {
        return FlagEligibility.allowed(new FlagEligibility.Context(player.isOnline(), data.frozen, player.hasPermission("linoac.bypass"),
                        data.bedrock && plugin.settings().enabled("settings.ignore-bedrock"),
                        plugin.settings().excludedWorlds().contains(player.getWorld().getName()), player.getPing(), plugin.tps()),
                plugin.settings().integer("settings.maximum-ping"), plugin.settings().number("settings.minimum-tps"));
    }

    public void reward(PlayerData data, CheckType type) {
        reward(data, type, plugin.settings().policy(type).decay());
    }

    public void reward(PlayerData data, CheckType type, double amount) {
        if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("Invalid finding decay.");
        data.buffers.computeIfPresent(type, (key, value) -> FindingBuffer.decayed(value, amount));
    }

    public void sample(Player player, PlayerData data, CheckType type, boolean suspicious, String detail) {
        if (suspicious) flag(player, data, type, detail); else reward(data, type);
    }

    public void flag(Player player, PlayerData data, CheckType type, String detail) {
        record(player, data, type, detail, FindingBuffer.Mode.AT_THRESHOLD);
    }

    public void direct(Player player, PlayerData data, CheckType type, String detail) {
        record(player, data, type, detail, FindingBuffer.Mode.DIRECT);
    }

    public void sampleAbove(Player player, PlayerData data, CheckType type, boolean suspicious, String detail) {
        if (suspicious) record(player, data, type, detail, FindingBuffer.Mode.ABOVE_THRESHOLD); else reward(data, type);
    }

    public void sampleAboveThenDecay(Player player, PlayerData data, CheckType type, String detail) {
        record(player, data, type, detail, FindingBuffer.Mode.ABOVE_WITH_DECAY);
    }

    public void sampleAboveOrDirect(Player player, PlayerData data, CheckType type, boolean immediate, String detail) {
        record(player, data, type, detail, FindingBuffer.Mode.ABOVE_THRESHOLD, immediate);
    }

    private void record(Player player, PlayerData data, CheckType type, String detail, FindingBuffer.Mode mode) {
        record(player, data, type, detail, mode, false);
    }

    private void record(Player player, PlayerData data, CheckType type, String detail, FindingBuffer.Mode mode, boolean immediate) {
        CheckPolicy policy = plugin.settings().policy(type);
        double currentBuffer = data.buffers.getOrDefault(type, 0d);
        var result = immediate ? FindingBuffer.advanceOrDirect(currentBuffer, policy.buffer(), true)
                : FindingBuffer.advance(currentBuffer, policy.buffer(), mode, policy.decay());
        data.buffers.put(type, FindingBuffer.submitted(result, policy.bufferMultiplier()));
        String bypass = "linoac.bypass." + type.id();
        if (!policy.enabled() || !eligible(player, data) || player.getPing() > policy.maximumPing() || plugin.tps() < policy.minimumTps()
                || player.isPermissionSet(bypass) && player.hasPermission(bypass)
                || data.automaticPunishment.blocked(System.nanoTime(), plugin.settings().integer("settings.automatic-punishment-cooldown-ms"))) return;
        long tick = plugin.tick();
        if (!result.ready()) {
            verbose(player, type, detail);
            return;
        }
        data.lastFlags.put(type, tick);
        int vl = Math.min(1000000, data.violations.getOrDefault(type, 0) + 1);
        LinoViolationEvent event = new LinoViolationEvent(player, type, detail, vl);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        data.violations.put(type, vl);
        data.historyTotals.recordAccepted(type.category(), type.family(), type.experimental());
        String plain = player.getName() + " failed " + type.id() + " VL=" + vl + " ping=" + player.getPing() + " " + detail;
        var context = plugin.actions().context(player, type, vl, detail);
        plugin.audit().write("violations", data.id, player.getName(), plugin.actions().render(plugin.settings().text("logs.violations.format"), context));
        long now = System.currentTimeMillis();
        if ((plugin.settings().integer("alerts.maximum-violations") == 0 || vl <= plugin.settings().integer("alerts.maximum-violations"))
                && AlertSelection.eligible(vl, policy.minimumAlertViolations(), policy.alertInterval())
                && now - data.lastAlerts.getOrDefault(type, 0L) >= plugin.settings().integer("settings.alert-cooldown-ms")) {
            data.lastAlerts.put(type, now);
            alert(player, type, vl, detail);
            if (plugin.settings().enabled("settings.console-alerts")) plugin.getLogger().info(plugin.actions().render(plugin.settings().text("alerts.console-format"), context));
        }
        if (vl % policy.webhookInterval() == 0) plugin.webhooks().send("alerts", "[LinoAC] " + plain, context);
        plugin.actions().applyFlagActions(player, type, vl, detail, policy);
        if (policy.setback() && vl >= policy.minimumSetbackViolations()) plugin.actions().applySetback(player, data.safe, type);
        if (!type.experimental() && policy.punishable() && plugin.settings().enabled("settings.automatic-punishments") && vl >= policy.maxViolations()
                && !(plugin.settings().enabled("wave.skip-queued-automatic-punishments") && plugin.store().wave().containsKey(data.id))) {
            String playerName = player.getName();
            plugin.actions().schedulePunishment(data.id, type.id(), Math.max(1, plugin.settings().integer("settings.punishment-delay-ticks")), () -> {
                CheckPolicy current = plugin.settings().policy(type);
                if (!plugin.settings().enabled("settings.automatic-punishments") || !current.enabled() || !current.punishable()) return;
                if (type.experimental() || plugin.settings().enabled("wave.skip-queued-automatic-punishments") && plugin.store().wave().containsKey(data.id)) return;
                Player online = Bukkit.getPlayer(data.id);
                if (online == null || plugin.data(online) != data || data.violations.getOrDefault(type, 0) < current.maxViolations()) return;
                if (data.automaticPunishment.blocked(System.nanoTime(), plugin.settings().integer("settings.automatic-punishment-cooldown-ms"))) return;
                if (plugin.settings().enabled("wave.queue-automatic-punishments")) {
                    if (plugin.store().wave().containsKey(data.id)) return;
                    try { plugin.punishments().add(data.id, playerName); data.lastPunishments.put(type, plugin.tick()); }
                    catch (java.io.IOException e) { plugin.getLogger().severe("Unable to queue punishment: " + e.getMessage()); }
                } else if (plugin.punishments().executeAutomatic(data.id, playerName, type.id(), current.commands())) {
                    data.lastPunishments.put(type, plugin.tick());
                    if (current.broadcastPunishment()) plugin.actions().broadcast(java.util.List.of(plugin.settings().text("messages.punishment-broadcast")), context);
                }
            });
        }
        Bukkit.getPluginManager().callEvent(new io.github.lino9999.linoac.api.LinoPostViolationEvent(data.id, player.getName(), type, vl, detail));
    }

    public void alert(Player suspect, CheckType type, int vl, String detail) {
        var context = plugin.actions().context(suspect, type, vl, detail);
        plugin.proxy().alert(suspect, new io.github.lino9999.linoac.integration.ProxyPacketCodec.AlertFields(type.displayName(),
                type.displayType(), Integer.toString(vl), suspect.getName(), context.get("max-vl"), context.get("client-version"),
                context.get("tps"), context.get("ping"), type.description(), detail, context.get("dev"), context.get("severity")));
        var event = new io.github.lino9999.linoac.api.LinoAlertEvent(suspect, type, vl, detail);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        TextComponent component = plugin.actions().createAlert(suspect, type, vl, detail);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            PlayerData staff = plugin.data(viewer);
            if (staff.alerts && viewer.hasPermission("linoac.alerts")) viewer.spigot().sendMessage(component);
        }
    }

    private void verbose(Player suspect, CheckType type, String detail) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (plugin.data(viewer).verbose && viewer.hasPermission("linoac.verbose")) {
                plugin.messages().send(viewer, "&8[Sample] &7" + suspect.getName() + " " + type.id() + " " + detail);
            }
        }
    }
}
