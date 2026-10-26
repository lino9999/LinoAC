package io.github.lino9999.linoac;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.lino9999.linoac.detection.DetectionEngine;
import io.github.lino9999.linoac.detection.ObservationCollector;
import io.github.lino9999.linoac.command.LinoCommand;
import io.github.lino9999.linoac.config.Settings;
import io.github.lino9999.linoac.listener.GameListener;
import io.github.lino9999.linoac.integration.BedrockSupport;
import io.github.lino9999.linoac.integration.LinoPlaceholders;
import io.github.lino9999.linoac.integration.WebhookService;
import io.github.lino9999.linoac.packet.ProtocolMonitor;
import io.github.lino9999.linoac.player.PlayerData;
import io.github.lino9999.linoac.service.Messages;
import io.github.lino9999.linoac.service.FeatureActions;
import io.github.lino9999.linoac.api.LinoAPI;
import io.github.lino9999.linoac.service.PunishmentService;
import io.github.lino9999.linoac.service.ViolationService;
import io.github.lino9999.linoac.storage.AuditLog;
import io.github.lino9999.linoac.storage.StateStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LinoAC extends JavaPlugin {
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private Settings settings;
    private StateStore store;
    private AuditLog audit;
    private Messages messages;
    private FeatureActions actions;
    private io.github.lino9999.linoac.service.GhostBlockRepair ghostBlocks;
    private ViolationService violations;
    private PunishmentService punishments;
    private DetectionEngine engine;
    private ObservationCollector collector;
    private ProtocolMonitor packets;
    private BedrockSupport bedrock;
    private io.github.lino9999.linoac.integration.OptionalMechanicsHooks mechanics;
    private WebhookService webhooks;
    private io.github.lino9999.linoac.integration.ProxyRelay proxy;
    private LinoPlaceholders expansion;
    private final Map<UUID, Map<String, String>> placeholderSnapshots = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile String tpsText = "20.00";
    private volatile String punishmentTotal = "0";
    private volatile long tick;
    private final long startedMillis = System.currentTimeMillis();
    private long previousTick;
    private double tickScale = 1;

    public void onEnable() {
        try {
            if (isFolia()) throw new IllegalStateException("This build requires the Spigot/Paper main-thread scheduler. Folia is not supported.");
            saveDefaultConfig();
            settings = new Settings(this);
            store = new StateStore(this);
            audit = new AuditLog(this, settings.integer("settings.log-retention-days"));
            configureAudit();
            messages = new Messages(this);
            actions = new FeatureActions(this);
            updatePunishmentSnapshot();
            mechanics = new io.github.lino9999.linoac.integration.OptionalMechanicsHooks(this, () -> settings.enabled("integrations.mcmmo"));
            mechanics.register();
            violations = new ViolationService(this);
            punishments = new PunishmentService(this);
            engine = new DetectionEngine(this);
            collector = new ObservationCollector(this, engine::accept);
            ghostBlocks = new io.github.lino9999.linoac.service.GhostBlockRepair(this);
            packets = new ProtocolMonitor(this);
            bedrock = new BedrockSupport();
            webhooks = new WebhookService(this);
            proxy = new io.github.lino9999.linoac.integration.ProxyRelay(this, () -> settings.enabled("proxy.enabled"),
                    () -> settings.text("proxy.channel"), () -> settings.text("proxy.alert-separator"));
            proxy.configure();
            PacketEvents.getAPI().getEventManager().registerListener(packets);
            Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
            LinoCommand command = new LinoCommand(this);
            Bukkit.getPluginManager().registerEvents(command, this);
            for (String name : new String[]{"linoac", "lacalerts", "lacwave", "laclogs", "lacpunishments"}) {
                var registered = java.util.Objects.requireNonNull(getCommand(name));
                registered.setExecutor(command);
                registered.setTabCompleter(command);
            }
            getServer().getMessenger().registerIncomingPluginChannel(this, "minecraft:brand", this::brand);
            Bukkit.getServicesManager().register(LinoAPI.class, new LinoAPI(this, actions), this, org.bukkit.plugin.ServicePriority.Normal);
            Bukkit.getOnlinePlayers().forEach(this::join);
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                expansion = new LinoPlaceholders(this);
                expansion.register();
            }
            previousTick = System.nanoTime();
            Bukkit.getScheduler().runTaskTimer(this, this::tickPlayers, 1, 1);
            getLogger().info("LinoAC " + getDescription().getVersion() + " by Lino enabled with " + engine.checkIds().size() + " operative checks and " + io.github.lino9999.linoac.check.CheckType.values().length + " catalog entries. Automatic punishments: " + settings.enabled("settings.automatic-punishments"));
        } catch (Exception e) {
            getLogger().severe("Unable to enable LinoAC: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        Bukkit.getServicesManager().unregisterAll(this);
        if (packets != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packets);
            packets.clear();
        }
        if (store != null) {
            players.forEach((id, data) -> store.setAlerts(id, data.alerts));
            try { store.save(); } catch (Exception e) { getLogger().severe("Unable to save state: " + e.getMessage()); }
        }
        players.clear();
        if (engine != null) engine.clear();
        if (collector != null) collector.clear();
        if (ghostBlocks != null) ghostBlocks.clear();
        placeholderSnapshots.clear();
        if (expansion != null) expansion.unregister();
        if (actions != null) actions.close();
        if (webhooks != null) webhooks.close();
        if (proxy != null) proxy.close();
        if (audit != null) audit.close();
    }

    private void tickPlayers() {
        tick++;
        long now = System.nanoTime();
        double elapsed = (now - previousTick) / 1_000_000d;
        previousTick = now;
        tickScale = Math.max(1, Math.min(5, elapsed / 50));
        double serverTps = tps();
        tpsText = Double.isFinite(serverTps) ? String.format(java.util.Locale.ROOT, "%.2f", serverTps) : "unknown";
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = data(player);
            if (elapsed > 110) exempt(data, 40);
            collector.tick(player);
            packets.tick(player, data);
            ghostBlocks.tick(player);
            data.attacked.clear();
            Map<String, String> snapshot = new HashMap<>();
            snapshot.putAll(Map.of("violations", Integer.toString(data.totalViolations()), "cps", Double.toString(data.clicksPerSecond),
                    "ping", Integer.toString(player.getPing()), "brand", data.brand, "client", data.clientVersion,
                    "frozen", Boolean.toString(data.frozen), "alerts", Boolean.toString(data.alerts), "uuid", data.id.toString()));
            Map<String, Double> metrics = collector.snapshot(player);
            for (String metric : java.util.List.of("transaction-ping", "pending-transactions", "transaction-age-ms", "movement-speed", "mouse-step-yaw", "mouse-step-pitch")) {
                Double value = metrics.get(metric);
                snapshot.put(metric.replace('-', '_'), value == null ? "unknown" : String.format(java.util.Locale.ROOT, "%.3f", value));
            }
            for (String category : java.util.List.of("combat", "movement", "player")) snapshot.put("violations_" + category,
                    Integer.toString(data.violations.entrySet().stream().filter(entry -> entry.getKey().category().equals(category)).mapToInt(Map.Entry::getValue).sum()));
            placeholderSnapshots.put(data.id, Map.copyOf(snapshot));
        }
        if (settings.enabled("settings.violation-reset-enabled") && tick % (settings.integer("settings.violation-reset-seconds") * 20L) == 0) actions.resetAll("LinoAC", true);
        punishments.tick(tick);
        actions.tick(tick);
        if (tick % 20 == 0) updatePunishmentSnapshot();
        if (tick % 1200 == 0) {
            try { store.save(); } catch (java.io.IOException e) { getLogger().warning("Unable to save state: " + e.getMessage()); }
        }
    }

    public void join(Player player) {
        PlayerData data = new PlayerData(player.getUniqueId(), tick + settings.integer("settings.join-grace-ticks"));
        data.alerts = store.alerts(data.id, settings.enabled("settings.alerts-on-join"));
        data.clientVersion = packets.clientVersion(player);
        data.bedrock = bedrock.isBedrock(data.id);
        players.put(data.id, data);
        packets.add(player);
        Bukkit.getPluginManager().callEvent(new io.github.lino9999.linoac.api.LinoPlayerRegisterEvent(data.id, player.getName()));
    }

    public void quit(Player player) {
        PlayerData data = players.remove(player.getUniqueId());
        placeholderSnapshots.remove(player.getUniqueId());
        packets.remove(player.getUniqueId());
        collector.remove(player.getUniqueId());
        ghostBlocks.remove(player.getUniqueId());
        engine.reset(player.getUniqueId());
        if (data == null) return;
        store.setAlerts(data.id, data.alerts);
        actions.onLogout(data.id, player.getName(), data.frozen);
        if (data.frozen) {
            getLogger().warning(player.getName() + " disconnected while frozen.");
            audit.write("violations", data.id, player.getName(), "Disconnected while frozen");
            for (Player staff : Bukkit.getOnlinePlayers()) if (staff.hasPermission("linoac.freeze")) messages.send(staff, "&c" + player.getName() + " disconnected while frozen.");
        }
    }

    private void brand(String channel, Player player, byte[] bytes) {
        if (!settings.enabled("brands.resolve") || bytes.length > 1024) return;
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
            int length = 0;
            int shift = 0;
            int value;
            do {
                if (shift >= 35) return;
                value = input.readUnsignedByte();
                length |= (value & 127) << shift;
                shift += 7;
            } while ((value & 128) != 0);
            if (length < 0 || length > 256 || length > input.available()) return;
            String brand = new String(input.readNBytes(length), java.nio.charset.StandardCharsets.UTF_8).replaceAll("[^A-Za-z0-9_. /+-]", "");
            PlayerData data = data(player);
            if (brand.equals(data.brand)) return;
            if (!actions.beforeClientBrand(player, brand)) return;
            data.brand = brand;
            String normalized = brand.toLowerCase(java.util.Locale.ROOT);
            boolean blocked = settings.list("brands.blocked").stream().filter(s -> !s.isBlank()).anyMatch(s -> normalized.contains(s.toLowerCase(java.util.Locale.ROOT)));
            boolean whitelisted = settings.list("brands.whitelist").stream().filter(s -> !s.isBlank()).anyMatch(s -> normalized.contains(s.toLowerCase(java.util.Locale.ROOT)));
            if (blocked || settings.enabled("brands.whitelist-enabled") && !whitelisted) {
                player.kickPlayer(messages.color(settings.text("messages.blocked-brand")));
                return;
            }
            if (settings.enabled("brands.alerts") && settings.list("brands.ignored").stream().filter(s -> !s.isBlank()).noneMatch(s -> normalized.contains(s.toLowerCase(java.util.Locale.ROOT)))) {
                String notice = actions.render(settings.text("brands.message"), Map.of("player", player.getName(), "brand", brand));
                for (Player staff : Bukkit.getOnlinePlayers()) if (data(staff).alerts && staff.hasPermission("linoac.alerts")) messages.send(staff, notice);
                if (settings.enabled("brands.console")) getLogger().info(org.bukkit.ChatColor.stripColor(messages.color(notice)));
            }
        } catch (java.io.IOException ignored) {
            getLogger().fine("Ignored malformed client brand from " + player.getName());
        }
    }

    public void reloadSettings() throws Exception {
        Settings replacement = new Settings(this);
        Settings previousSettings = settings;
        settings = replacement;
        try { proxy.configure(); }
        catch (RuntimeException failure) { settings = previousSettings; throw failure; }
        actions.cancelPendingPunishments();
        audit.retention(replacement.integer("settings.log-retention-days"));
        configureAudit();
        engine.clear();
        collector.clear();
        mechanics.register();
        players.values().forEach(data -> { data.reset(); exempt(data, 40); });
    }

    public PlayerData data(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), id -> new PlayerData(id, tick + settings.integer("settings.join-grace-ticks")));
    }

    private void configureAudit() {
        audit.configure(settings.enabled("logs.violations.enabled"), settings.enabled("logs.punishments.enabled"),
                (long) (settings.number("logs.maximum-file-mib") * 1024 * 1024));
    }

    public void exempt(PlayerData data, int ticks) {
        data.graceUntil = Math.max(data.graceUntil, tick + ticks);
        data.airTicks = 0;
        data.previousDy = 0;
    }

    public void exemptMovement(PlayerData data, int ticks) {
        data.movementGraceUntil = Math.max(data.movementGraceUntil, tick + ticks);
        data.airTicks = 0;
        data.previousDy = 0;
    }

    private boolean isFolia() {
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return true; }
        catch (ClassNotFoundException e) { return false; }
    }

    public Settings settings() { return settings; }
    public StateStore store() { return store; }
    public AuditLog audit() { return audit; }
    public Messages messages() { return messages; }
    public ViolationService violations() { return violations; }
    public PunishmentService punishments() { return punishments; }
    public long tick() { return tick; }
    public long startedMillis() { return startedMillis; }
    public double tps() { return Math.min(20, io.github.retrooper.packetevents.util.SpigotReflectionUtil.getTPS()); }
    public double tickScale() { return tickScale; }
    public WebhookService webhooks() { return webhooks; }
    public ObservationCollector collector() { return collector; }
    public DetectionEngine engine() { return engine; }
    public FeatureActions actions() { return actions; }
    public io.github.lino9999.linoac.integration.ProxyRelay proxy() { return proxy; }
    public void serverMoved(Player player, org.bukkit.Location destination) {
        var nearby = player.getNearbyEntities(1.5, 2, 1.5);
        var snapshot = collector.serverMoved(player, destination, nearby);
        packets.serverWorld(player, snapshot);
    }
    public io.github.lino9999.linoac.integration.OptionalMechanicsHooks mechanics() { return mechanics; }
    public String placeholder(UUID id, String identifier) {
        identifier = switch (identifier) {
            case "total_violations" -> "violations";
            case "combat_violations" -> "violations_combat";
            case "movement_violations" -> "violations_movement";
            case "player_violations" -> "violations_player";
            case "client_brand" -> "brand";
            default -> identifier;
        };
        if (identifier.equals("total_punishments")) return punishmentTotal;
        if (identifier.equals("tps")) return tpsText;
        if (identifier.equals("version")) return getDescription().getVersion();
        Map<String, String> snapshot = id == null ? null : placeholderSnapshots.get(id);
        return snapshot == null ? "" : snapshot.get(identifier);
    }

    private void updatePunishmentSnapshot() {
        var total = actions.statistics().persistedTotal();
        punishmentTotal = total.isPresent() ? Long.toString(total.getAsLong()) : "unknown";
    }
}
