package io.github.lino9999.linoac.service;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.api.LinoPunishmentEvent;
import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.integration.ProxyPacketCodec;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

public final class PunishmentService {
    private final LinoAC plugin;
    private final java.util.ArrayDeque<Map.Entry<UUID, String>> running = new java.util.ArrayDeque<>();
    private long nextEntryTick;
    private int waveAttempted, waveCompleted;
    private boolean waveActive;
    private boolean startingWave;
    private boolean processingEntry;

    public PunishmentService(LinoAC plugin) { this.plugin = plugin; }

    public boolean execute(UUID id, String name, String reason, List<String> templates) {
        return execute(id, name, reason, templates, reason.equals("wave") ? Origin.WAVE : Origin.MANUAL);
    }

    public boolean executeAutomatic(UUID id, String name, String reason, List<String> templates) {
        return execute(id, name, reason, templates, Origin.AUTOMATIC);
    }

    private boolean execute(UUID id, String name, String reason, List<String> templates, Origin origin) {
        if (!name.matches("[A-Za-z0-9_]{1,16}")) return false;
        Map<String, String> webhookContext = new LinkedHashMap<>();
        webhookContext.put("player", name);
        webhookContext.put("uuid", id.toString());
        webhookContext.put("check", reason);
        var online = Bukkit.getPlayer(id);
        var subject = online != null && online.isOnline() ? plugin.data(online) : null;
        if (origin == Origin.AUTOMATIC && subject != null
                && subject.automaticPunishment.blocked(System.nanoTime(), plugin.settings().integer("settings.automatic-punishment-cooldown-ms"))) return false;
        if (online != null && online.isOnline()) {
            var profile = plugin.data(online);
            var location = online.getLocation();
            webhookContext.put("client-version", profile.clientVersion);
            webhookContext.put("client-brand", profile.brand);
            webhookContext.put("ping", Integer.toString(online.getPing()));
            webhookContext.put("tps", String.format(java.util.Locale.ROOT, "%.2f", plugin.tps()));
            webhookContext.put("world", online.getWorld().getName());
            webhookContext.put("x", Double.toString(location.getX()));
            webhookContext.put("y", Double.toString(location.getY()));
            webhookContext.put("z", Double.toString(location.getZ()));
        }
        List<String> commands = templates.stream().map(s -> s.replace("%player%", name)
                .replace("%uuid%", id.toString()).replace("%check%", reason)).toList();
        LinoPunishmentEvent event = new LinoPunishmentEvent(id, name, reason, commands);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        boolean dispatched = false;
        for (String command : commands) {
            boolean result = false;
            String executable = command.startsWith("/") ? command.substring(1) : command;
            boolean valid = !executable.isBlank() && executable.indexOf('\n') < 0 && executable.indexOf('\r') < 0 && executable.indexOf('\0') < 0;
            try {
                if (valid) {
                    var remote = origin == Origin.WAVE ? java.util.Optional.<String>empty() : ProxyPacketCodec.remoteCommand(executable);
                    if (remote.isEmpty()) result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), executable);
                    else if (origin == Origin.MANUAL) result = plugin.proxy().manualCommand(online, remote.get());
                    else {
                        CheckType type = CheckType.find(reason).orElseThrow(() -> new IllegalArgumentException("Unknown automatic punishment check."));
                        int violations = online == null ? 0 : plugin.data(online).violations.getOrDefault(type, 0);
                        result = plugin.proxy().automaticCommand(online, remote.get(), new ProxyPacketCodec.PunishmentFields(type.displayName(),
                                type.displayType(), Integer.toString(violations), name, Integer.toString(plugin.settings().policy(type).maxViolations())));
                    }
                }
            }
            catch (RuntimeException e) { plugin.getLogger().warning("A punishment command failed for " + name + "."); }
            dispatched |= result;
            String format = plugin.settings().text("logs.punishments.format");
            if (format.isBlank()) format = "reason=%check% command=%command% dispatched=%dispatched%";
            String message = plugin.actions().render(format, Map.of("player", name, "uuid", id.toString(),
                    "check", reason, "command", command, "dispatched", Boolean.toString(result)));
            var logEvent = new io.github.lino9999.linoac.api.LinoPunishmentLogEvent(id, name, message);
            Bukkit.getPluginManager().callEvent(logEvent);
            if (!logEvent.isCancelled()) plugin.audit().write("punishments", id, name, message);
        }
        if (dispatched) {
            if (origin == Origin.AUTOMATIC && subject != null) {
                subject.violations.clear();
                subject.automaticPunishment.accepted(System.nanoTime());
            }
            plugin.actions().recordPunishment(id, name, reason);
            plugin.webhooks().send("punishments", "[LinoAC] " + name + " punishment commands dispatched: " + reason, webhookContext);
        }
        return dispatched;
    }

    private enum Origin { MANUAL, AUTOMATIC, WAVE }

    public void add(UUID id, String name) throws IOException {
        String previous = plugin.store().wave().put(id, name);
        try { plugin.store().save(); }
        catch (IOException e) {
            if (previous == null) plugin.store().wave().remove(id); else plugin.store().wave().put(id, previous);
            throw e;
        }
    }

    public boolean remove(UUID id) throws IOException {
        return removeQueued(id, true);
    }

    private boolean removeQueued(UUID id, boolean notifyEnd) throws IOException {
        String name = plugin.store().wave().remove(id);
        if (name != null) {
            try { plugin.store().save(); }
            catch (IOException e) { plugin.store().wave().put(id, name); throw e; }
        }
        running.removeIf(entry -> entry.getKey().equals(id));
        if (notifyEnd && waveActive && running.isEmpty() && !processingEntry) finishWave();
        return name != null;
    }

    public int startWave() {
        return startWave("LinoAC");
    }

    public int startWave(String actor) {
        if (startingWave || waveActive || !running.isEmpty() || plugin.store().wave().isEmpty()) return 0;
        Map<UUID, String> snapshot = new LinkedHashMap<>(plugin.store().wave());
        startingWave = true;
        try {
            if (!plugin.actions().beforeWaveStart(snapshot, actor)) return 0;
            snapshot.forEach((id, name) -> {
                if (name.equals(plugin.store().wave().get(id))) running.add(Map.entry(id, name));
            });
            waveAttempted = waveCompleted = 0;
            waveActive = true;
            nextEntryTick = plugin.tick();
            int size = running.size();
            if (size == 0) finishWave();
            return size;
        } finally { startingWave = false; }
    }

    public void tick(long tick) {
        if (plugin.settings().enabled("wave.automatic") && tick % (plugin.settings().integer("wave.interval-seconds") * 20L) == 0) startWave("Scheduler");
        if (running.isEmpty() || tick < nextEntryTick) return;
        var entry = running.peek();
        waveAttempted++;
        processingEntry = true;
        try {
            var commands = plugin.settings().list("wave.commands");
            if (!plugin.actions().beforeWaveEntry(entry.getKey(), entry.getValue(), commands)) running.removeFirst();
            else if (removeQueued(entry.getKey(), false)) {
                if (execute(entry.getKey(), entry.getValue(), "wave", commands)) {
                    waveCompleted++;
                    plugin.actions().broadcast(plugin.settings().list("wave.entry-broadcast"),
                            Map.of("player", entry.getValue(), "uuid", entry.getKey().toString(), "check", "wave"));
                }
            }
        } catch (IOException e) {
            running.clear();
            plugin.getLogger().severe("Wave paused: " + e.getMessage());
        } finally { processingEntry = false; }
        nextEntryTick = tick + Math.max(1, plugin.settings().integer("wave.entry-delay-ticks"));
        if (running.isEmpty() && waveActive) finishWave();
    }

    private void finishWave() {
        int attempted = waveAttempted;
        int completed = waveCompleted;
        waveActive = false;
        waveAttempted = waveCompleted = 0;
        plugin.actions().afterWaveEnd(attempted, completed);
    }
}
