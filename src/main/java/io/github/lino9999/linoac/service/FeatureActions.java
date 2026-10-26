package io.github.lino9999.linoac.service;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.api.LinoAlertToggleEvent;
import io.github.lino9999.linoac.api.LinoCheckToggleEvent;
import io.github.lino9999.linoac.api.LinoClientBrandEvent;
import io.github.lino9999.linoac.api.LinoFreezeEvent;
import io.github.lino9999.linoac.api.LinoResetEvent;
import io.github.lino9999.linoac.api.LinoSetbackEvent;
import io.github.lino9999.linoac.api.LinoWaveEndEvent;
import io.github.lino9999.linoac.api.LinoWaveEntryEvent;
import io.github.lino9999.linoac.api.LinoWaveStartEvent;
import io.github.lino9999.linoac.check.CheckPolicy;
import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.player.PlayerData;
import io.github.lino9999.linoac.detection.AlertSelection;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeatureActions implements AutoCloseable {
    private static final Pattern TOKEN = Pattern.compile("%([A-Za-z0-9_-]+)%");
    private final LinoAC plugin;
    private final Map<UUID, Long> hitSuppression = new HashMap<>();
    private final PendingPunishmentActions<BukkitTask> pendingPunishments = new PendingPunishmentActions<>(BukkitTask::cancel);
    private final Map<LocalDate, Long> punishmentDays = new TreeMap<>();
    private final Path statisticsPath;
    private final ThreadPoolExecutor statisticsWriter;
    private long sessionPunishments;
    private long persistedPunishments;
    private boolean statisticsKnown = true;
    private long waveStarted;
    private boolean closed;

    public FeatureActions(LinoAC plugin) {
        this.plugin = java.util.Objects.requireNonNull(plugin);
        statisticsPath = plugin.getDataFolder().toPath().resolve("statistics.yml");
        statisticsWriter = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), action -> {
            Thread thread = new Thread(action, "LinoAC-statistics");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardOldestPolicy());
        loadStatistics();
    }

    public boolean setAlerts(Player player, boolean enabled) {
        primaryThread();
        PlayerData data = plugin.data(player);
        if (data.alerts == enabled) return true;
        LinoAlertToggleEvent event = new LinoAlertToggleEvent(data.id, player.getName(), enabled);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        boolean previous = data.alerts;
        data.alerts = enabled;
        plugin.store().setAlerts(data.id, enabled);
        try { plugin.store().save(); }
        catch (IOException e) {
            data.alerts = previous;
            plugin.store().setAlerts(data.id, previous);
            throw new UncheckedIOException(e);
        }
        return true;
    }

    public boolean setFrozen(Player player, boolean frozen, String actor) {
        primaryThread();
        PlayerData data = plugin.data(player);
        if (data.frozen == frozen) return true;
        LinoFreezeEvent event = new LinoFreezeEvent(data.id, player.getName(), frozen, actor);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        data.frozen = frozen;
        plugin.exemptMovement(data, 60);
        plugin.messages().send(player, frozen ? "&cYou are frozen. Please await instructions from staff." : "&aYou are no longer frozen.");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("linoac.freeze")) plugin.messages().send(staff,
                    "&f" + player.getName() + " &7was " + (frozen ? "frozen" : "unfrozen") + " by &f" + actor + "&7.");
        }
        return true;
    }

    public boolean beforeClientBrand(Player player, String brand) {
        primaryThread();
        LinoClientBrandEvent event = new LinoClientBrandEvent(player.getUniqueId(), player.getName(), plugin.data(player).brand, brand);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public boolean beforeCheckToggle(CheckType check, boolean enabled, String actor) {
        primaryThread();
        if (enabled && !check.available()) return false;
        LinoCheckToggleEvent event = new LinoCheckToggleEvent(check, enabled, actor);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public boolean applySetback(Player player, Location target, CheckType check) {
        primaryThread();
        if (target == null || target.getWorld() == null || !target.getWorld().equals(player.getWorld())) return false;
        target.checkFinite();
        if (!target.getWorld().isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) return false;
        Location destination = target.clone();
        LinoSetbackEvent event = new LinoSetbackEvent(player.getUniqueId(), player.getName(), check, player.getLocation(), destination);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || !player.teleport(destination)) return false;
        plugin.exemptMovement(plugin.data(player), 20);
        return true;
    }

    public boolean reset(Player player, String actor, boolean automatic) {
        primaryThread();
        LinoResetEvent event = new LinoResetEvent(player.getUniqueId(), actor, automatic);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        cancelPendingPunishments(player.getUniqueId());
        plugin.data(player).reset();
        if (plugin.engine() != null) plugin.engine().reset(player.getUniqueId());
        return true;
    }

    public boolean resetAll(String actor, boolean automatic) {
        primaryThread();
        LinoResetEvent event = new LinoResetEvent(null, actor, automatic);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        cancelPendingPunishments();
        Bukkit.getOnlinePlayers().forEach(player -> plugin.data(player).reset());
        if (plugin.engine() != null) plugin.engine().clear();
        String message = plugin.settings().text("messages.violation-reset");
        if (!message.isBlank()) {
            for (Player staff : Bukkit.getOnlinePlayers()) if (staff.hasPermission("linoac.alerts") && plugin.data(staff).alerts) {
                plugin.messages().send(staff, render(message, Map.of("staff", actor)));
            }
        }
        return true;
    }

    public boolean beforeWaveStart(Map<UUID, String> entries, String actor) {
        primaryThread();
        LinoWaveStartEvent event = new LinoWaveStartEvent(actor, entries);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        waveStarted = System.nanoTime();
        broadcast(plugin.settings().list("wave.started-broadcast"), Map.of("amount", Integer.toString(entries.size()), "staff", actor));
        return true;
    }

    public boolean beforeWaveEntry(UUID id, String name, List<String> commands) {
        primaryThread();
        LinoWaveEntryEvent event = new LinoWaveEntryEvent(id, name, commands);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public void afterWaveEnd(int attempted, int completed) {
        primaryThread();
        long duration = waveStarted == 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - waveStarted);
        waveStarted = 0;
        Bukkit.getPluginManager().callEvent(new LinoWaveEndEvent(attempted, completed, duration));
        broadcast(plugin.settings().list("wave.ended-broadcast"), Map.of("amount", Integer.toString(completed), "attempted", Integer.toString(attempted)));
    }

    public void applyFlagActions(Player player, CheckType type, int violations, String detail, CheckPolicy policy) {
        primaryThread();
        boolean shuffle = policy.shuffleMinimum() > 0 && violations >= policy.shuffleMinimum() && violations % Math.max(1, policy.shuffleInterval()) == 0;
        boolean rotate = policy.rotateMinimum() > 0 && violations >= policy.rotateMinimum() && violations % Math.max(1, policy.rotateInterval()) == 0;
        applyFlagActions(player, type, violations, detail, new FlagActions(AlertSelection.eligible(violations, policy.minimumAlertViolations(), policy.alertInterval())
                ? plugin.settings().list("alerts.custom-commands") : List.of(),
                plugin.settings().enabled("settings.cancel-hits") && (plugin.settings().list("settings.cancel-hits-checks").isEmpty()
                        || plugin.settings().list("settings.cancel-hits-checks").contains(type.id())) ? plugin.settings().integer("settings.cancel-hits-ms") : 0, shuffle, rotate));
    }

    public void applyFlagActions(Player player, CheckType type, int violations, String detail, FlagActions actions) {
        primaryThread();
        runCommands(actions.commands(), context(player, type, violations, detail));
        if (actions.hitCancelMillis() > 0) hitSuppression.put(player.getUniqueId(), System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.min(3600000, actions.hitCancelMillis())));
        if (actions.shuffle()) shuffle(player);
        if (actions.rotate()) rotate(player);
    }

    public boolean shouldCancelHit(Player player) {
        primaryThread();
        Long deadline = hitSuppression.get(player.getUniqueId());
        if (deadline == null) return false;
        if (System.nanoTime() < deadline) return true;
        hitSuppression.remove(player.getUniqueId());
        return false;
    }

    public boolean schedulePunishment(UUID id, String reason, long delayTicks, Runnable action) {
        primaryThread();
        java.util.Objects.requireNonNull(reason);
        java.util.Objects.requireNonNull(action);
        if (closed) return false;
        var ticket = pendingPunishments.reserve(id);
        if (ticket == null) return false;
        if (delayTicks <= 0) { pendingPunishments.release(ticket); action.run(); return true; }
        try {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (pendingPunishments.release(ticket) && plugin.isEnabled()) action.run();
            }, Math.min(delayTicks, 72000));
            pendingPunishments.attach(ticket, task);
        } catch (RuntimeException failure) {
            pendingPunishments.release(ticket);
            throw failure;
        }
        return true;
    }

    public void onLogout(UUID id, String name, boolean frozen) {
        primaryThread();
        hitSuppression.remove(id);
        cancelPendingPunishments(id);
        if (frozen) runCommands(plugin.settings().list("freeze.logout-commands"), Map.of("player", name, "uuid", id.toString()));
    }

    private void cancelPendingPunishments(UUID id) {
        pendingPunishments.cancel(id);
    }

    public void cancelPendingPunishments() {
        primaryThread();
        pendingPunishments.cancelAll();
    }

    public void shuffle(Player player) {
        primaryThread();
        List<ItemStack> hotbar = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            hotbar.add(item == null ? null : item.clone());
        }
        Collections.shuffle(hotbar);
        for (int slot = 0; slot < 9; slot++) player.getInventory().setItem(slot, hotbar.get(slot));
    }

    public void rotate(Player player) {
        primaryThread();
        Location location = player.getLocation();
        location.setYaw(ThreadLocalRandom.current().nextFloat(-180, 180));
        location.setPitch(ThreadLocalRandom.current().nextFloat(-90, 90));
        player.teleport(location);
    }

    public Map<String, String> context(Player player, CheckType type, int violations, String detail) {
        primaryThread();
        PlayerData data = plugin.data(player);
        Location location = player.getLocation();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("prefix", plugin.settings().text("messages.prefix"));
        values.put("player", player.getName());
        values.put("name", player.getName());
        values.put("uuid", player.getUniqueId().toString());
        values.put("check", type.id());
        values.put("check-id", type.id());
        values.put("check-family", type.family());
        values.put("type", type.id().substring(type.id().lastIndexOf('-') + 1).toUpperCase(Locale.ROOT));
        values.put("description", type.description());
        values.put("vl", Integer.toString(violations));
        values.put("max-vl", Integer.toString(plugin.settings().policy(type).maxViolations()));
        values.put("ping", Integer.toString(player.getPing()));
        values.put("tps", String.format(Locale.ROOT, "%.2f", plugin.tps()));
        values.put("version", data.clientVersion);
        values.put("client-version", data.clientVersion);
        values.put("client-brand", data.brand);
        values.put("plugin-version", plugin.getDescription().getVersion());
        values.put("server-version", Bukkit.getBukkitVersion());
        values.put("server-name", plugin.settings().text("settings.server-name"));
        values.put("x", String.format(Locale.ROOT, "%.2f", location.getX()));
        values.put("y", String.format(Locale.ROOT, "%.2f", location.getY()));
        values.put("z", String.format(Locale.ROOT, "%.2f", location.getZ()));
        values.put("world", player.getWorld().getName());
        values.put("info", detail);
        values.put("dev", type.experimental() ? plugin.settings().text("alerts.experimental-symbol") : "");
        values.put("severity", severity(violations));
        return Map.copyOf(values);
    }

    public TextComponent createAlert(Player player, CheckType type, int violations, String detail) {
        primaryThread();
        Map<String, String> values = context(player, type, violations, detail);
        String format = plugin.settings().text("alerts.format");
        if (format.isBlank()) format = plugin.settings().text("messages.alert");
        if (!format.contains("%prefix%")) format = "%prefix% " + format;
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(plugin.messages().color(render(format, values))));
        List<String> hover = plugin.settings().list("alerts.hover");
        if (hover.isEmpty()) hover = List.of("Ping: %ping%ms | TPS: %tps%", "%description%", "%info%", "Click to teleport.");
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(plugin.messages().color(render(String.join("\n", hover), values)))));
        String click = plugin.settings().text("alerts.click-command");
        if (click.isBlank()) click = "/linoac teleport %player%";
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, render(click, values)));
        return component;
    }

    public String render(String template, Map<String, String> values) {
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(values.getOrDefault(matcher.group(1), matcher.group())));
        matcher.appendTail(result);
        return result.toString();
    }

    public int runCommands(List<String> templates, Map<String, String> values) {
        primaryThread();
        int dispatched = 0;
        for (String template : templates) {
            String command = render(template, values).trim();
            if (command.startsWith("/")) command = command.substring(1);
            if (command.isBlank() || command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0 || command.indexOf('\0') >= 0) continue;
            try { if (Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) dispatched++; }
            catch (RuntimeException e) { plugin.getLogger().warning("A configured action command failed to execute."); }
        }
        return dispatched;
    }

    public void broadcast(List<String> templates, Map<String, String> values) {
        primaryThread();
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("prefix", plugin.settings().text("messages.prefix"));
        placeholders.putAll(values);
        for (String template : templates) Bukkit.broadcastMessage(plugin.messages().color(render(template, placeholders)));
    }

    public void recordPunishment(UUID id, String name, String reason) {
        primaryThread();
        sessionPunishments = increment(sessionPunishments);
        if (!statisticsKnown) return;
        persistedPunishments = increment(persistedPunishments);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        punishmentDays.merge(today, 1L, this::add);
        pruneStatistics(today);
        StringBuilder text = new StringBuilder("format-version: 1\ntotal: ").append(persistedPunishments).append("\ndays:\n");
        punishmentDays.forEach((date, count) -> text.append("  '").append(date).append("': ").append(count).append('\n'));
        String snapshot = text.toString();
        statisticsWriter.execute(() -> saveStatistics(snapshot));
    }

    public Statistics statistics() {
        primaryThread();
        if (!statisticsKnown) return new Statistics(sessionPunishments, OptionalLong.empty(), OptionalLong.empty());
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        pruneStatistics(today);
        long recent = 0;
        for (Map.Entry<LocalDate, Long> entry : punishmentDays.entrySet()) if (!entry.getKey().isAfter(today)) recent = add(recent, entry.getValue());
        return new Statistics(sessionPunishments, OptionalLong.of(persistedPunishments), OptionalLong.of(recent));
    }

    public void tick(long tick) {
        primaryThread();
        if (tick % 200 == 0) {
            long now = System.nanoTime();
            hitSuppression.values().removeIf(deadline -> deadline <= now);
        }
        long interval = Math.max(1, plugin.settings().integer("settings.statistics-interval-seconds")) * 20L;
        if (plugin.settings().enabled("settings.statistics-broadcast") && tick % interval == 0) {
            Statistics statistics = statistics();
            String message = plugin.settings().text("messages.statistics-broadcast");
            if (!message.isBlank()) broadcast(List.of(message), Map.of(
                    "amount", statistics.lastSevenCalendarDays().isPresent() ? Long.toString(statistics.lastSevenCalendarDays().getAsLong()) : "unknown",
                    "total", statistics.persistedTotal().isPresent() ? Long.toString(statistics.persistedTotal().getAsLong()) : "unknown",
                    "session", Long.toString(statistics.sessionTotal())));
        }
    }

    private String severity(int violations) {
        if (!plugin.settings().enabled("alerts.severity.enabled")) return "";
        String[] defaults = {"&a", "&e", "&6", "&c", "&4"};
        int level = 1;
        for (int candidate = 2; candidate <= 10; candidate++) {
            int threshold = plugin.settings().integer("alerts.severity.violations." + candidate);
            if (threshold <= 0 && candidate <= 5) threshold = (candidate - 1) * 4;
            if (threshold > 0 && violations >= threshold) level = candidate;
        }
        String color = plugin.settings().text("alerts.severity.colors." + level);
        return color.isBlank() ? defaults[Math.min(defaults.length, level) - 1] : color;
    }

    private void loadStatistics() {
        if (!Files.exists(statisticsPath)) return;
        try {
            YamlConfiguration data = new YamlConfiguration();
            data.load(statisticsPath.toFile());
            Object total = data.get("total");
            if (!(total instanceof Long || total instanceof Integer) || ((Number) total).longValue() < 0) throw new IOException("Invalid punishment total.");
            Number number = (Number) total;
            persistedPunishments = number.longValue();
            var days = data.getConfigurationSection("days");
            if (days != null) for (String day : days.getKeys(false)) {
                Object count = days.get(day);
                if (!(count instanceof Long || count instanceof Integer) || ((Number) count).longValue() < 0) throw new IOException("Invalid punishment day count.");
                Number value = (Number) count;
                punishmentDays.put(LocalDate.parse(day), value.longValue());
            }
            pruneStatistics(LocalDate.now(ZoneOffset.UTC));
        } catch (Exception e) {
            statisticsKnown = false;
            punishmentDays.clear();
            plugin.getLogger().warning("Punishment statistics could not be restored; historical counts are unavailable.");
        }
    }

    private void saveStatistics(String snapshot) {
        try {
            Path temporary = statisticsPath.resolveSibling("statistics.yml.tmp");
            Files.writeString(temporary, snapshot, StandardCharsets.UTF_8);
            try { Files.move(temporary, statisticsPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, statisticsPath, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { plugin.getLogger().warning("Unable to persist punishment statistics."); }
    }

    private void pruneStatistics(LocalDate today) { punishmentDays.keySet().removeIf(date -> date.isBefore(today.minusDays(6))); }
    private long increment(long value) { return value == Long.MAX_VALUE ? value : value + 1; }
    private long add(long left, long right) { return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right; }

    private void primaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("This LinoAC action requires the server main thread.");
    }

    public void close() {
        primaryThread();
        closed = true;
        cancelPendingPunishments();
        hitSuppression.clear();
        statisticsWriter.shutdown();
        try {
            if (!statisticsWriter.awaitTermination(5, TimeUnit.SECONDS)) {
                statisticsWriter.shutdownNow();
                plugin.getLogger().warning("Statistics shutdown timeout; the newest count may not have reached disk.");
            }
        } catch (InterruptedException e) { statisticsWriter.shutdownNow(); Thread.currentThread().interrupt(); }
    }

    public record FlagActions(List<String> commands, long hitCancelMillis, boolean shuffle, boolean rotate) {
        public FlagActions { commands = List.copyOf(commands); }
    }

    public record Statistics(long sessionTotal, OptionalLong persistedTotal, OptionalLong lastSevenCalendarDays) {}
}
