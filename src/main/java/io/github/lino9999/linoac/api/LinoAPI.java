package io.github.lino9999.linoac.api;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.check.CheckPolicy;
import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.player.PlayerData;
import io.github.lino9999.linoac.service.FeatureActions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.UUID;

public final class LinoAPI {
    private final LinoAC plugin;
    private final FeatureActions actions;

    public LinoAPI(LinoAC plugin, FeatureActions actions) {
        this.plugin = java.util.Objects.requireNonNull(plugin);
        this.actions = java.util.Objects.requireNonNull(actions);
    }

    public static LinoAPI get() {
        LinoAPI api = Bukkit.getServicesManager().load(LinoAPI.class);
        if (api == null) throw new IllegalStateException("LinoAC API is not registered.");
        return api;
    }

    public String version() { return plugin.getDescription().getVersion(); }

    public List<CheckDefinition> catalogue() {
        return Arrays.stream(CheckType.values()).map(this::definition).toList();
    }

    public Optional<Profile> profile(UUID id) {
        primaryThread();
        Player player = Bukkit.getPlayer(id);
        return player == null ? Optional.empty() : Optional.of(snapshot(player));
    }

    public List<Profile> onlineProfiles() {
        primaryThread();
        return Bukkit.getOnlinePlayers().stream().map(this::snapshot).toList();
    }

    public Optional<Map<String, Double>> measurements(UUID id) {
        primaryThread();
        Player player = Bukkit.getPlayer(java.util.Objects.requireNonNull(id));
        return player == null ? Optional.empty() : Optional.of(Map.copyOf(plugin.collector().snapshot(player)));
    }

    public Policy policy(CheckType type) {
        primaryThread();
        CheckPolicy policy = plugin.settings().policy(type);
        return new Policy(policy.enabled(), policy.buffer(), policy.decay(), policy.maxViolations(), policy.setback(),
                policy.punishable(), policy.commands(), policy.bufferMultiplier(), policy.minimumSetbackViolations(),
                policy.alertInterval(), policy.minimumAlertViolations(), policy.maximumPing(), policy.minimumTps(),
                policy.webhookInterval(), policy.broadcastPunishment(), policy.shuffleMinimum(), policy.shuffleInterval(),
                policy.rotateMinimum(), policy.rotateInterval());
    }

    public Optional<CheckState> check(UUID id, CheckType type) {
        primaryThread();
        Player player = Bukkit.getPlayer(id);
        if (player == null) return Optional.empty();
        PlayerData data = plugin.data(player);
        return Optional.of(new CheckState(definition(type), policy(type), data.violations.getOrDefault(type, 0),
                data.buffers.getOrDefault(type, 0d)));
    }

    public boolean setFrozen(UUID id, boolean frozen, String actor) {
        primaryThread();
        return actions.setFrozen(online(id), frozen, actor);
    }

    public boolean setAlerts(UUID id, boolean enabled) {
        primaryThread();
        return actions.setAlerts(online(id), enabled);
    }

    public void setVerbose(UUID id, boolean enabled) {
        primaryThread();
        plugin.data(online(id)).verbose = enabled;
    }

    public boolean reset(UUID id, String actor) {
        primaryThread();
        return actions.reset(online(id), actor, false);
    }

    public boolean resetAll(String actor) {
        primaryThread();
        return actions.resetAll(actor, false);
    }

    public boolean setback(UUID id, org.bukkit.Location target, CheckType check) {
        primaryThread();
        return actions.applySetback(online(id), target, check);
    }

    public void queueWave(UUID id) {
        primaryThread();
        Player player = online(id);
        try { plugin.punishments().add(id, player.getName()); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public boolean removeWaveEntry(UUID id) {
        primaryThread();
        try { return plugin.punishments().remove(id); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public Map<UUID, String> waveQueue() {
        primaryThread();
        return Map.copyOf(plugin.store().wave());
    }

    public int startWave() {
        primaryThread();
        return plugin.punishments().startWave("API");
    }

    public void flag(UUID id, String checkId, String detail) {
        primaryThread();
        java.util.Objects.requireNonNull(checkId, "checkId");
        java.util.Objects.requireNonNull(detail, "detail");
        Player player = online(id);
        CheckType type = CheckType.find(checkId).orElseThrow(() -> new IllegalArgumentException("Unknown check ID: " + checkId));
        if (!type.available()) throw new IllegalArgumentException("This check has no executable implementation in the supplied artifact.");
        plugin.violations().flag(player, plugin.data(player), type, detail);
    }

    public Statistics statistics() {
        primaryThread();
        FeatureActions.Statistics snapshot = actions.statistics();
        return new Statistics(snapshot.sessionTotal(), snapshot.persistedTotal(), snapshot.lastSevenCalendarDays());
    }

    private CheckDefinition definition(CheckType type) {
        return new CheckDefinition(type.id(), type.family(), type.category(), type.description(), type.available());
    }

    private Profile snapshot(Player player) {
        PlayerData data = plugin.data(player);
        Map<String, Integer> violations = new LinkedHashMap<>();
        Map<String, Double> buffers = new LinkedHashMap<>();
        Map<String, Integer> categories = new TreeMap<>();
        for (CheckType type : CheckType.values()) {
            categories.putIfAbsent(type.category(), 0);
            int count = data.violations.getOrDefault(type, 0);
            if (count > 0) violations.put(type.id(), count);
            double buffer = data.buffers.getOrDefault(type, 0d);
            if (buffer > 0) buffers.put(type.id(), buffer);
            categories.merge(type.category(), count, Integer::sum);
        }
        return new Profile(data.id, player.getName(), player.getPing(), data.brand, data.clientVersion, data.swings,
                data.totalViolations(), categories, violations, buffers, data.frozen, data.alerts, data.verbose);
    }

    private Player online(UUID id) {
        Player player = Bukkit.getPlayer(java.util.Objects.requireNonNull(id));
        if (player == null) throw new IllegalArgumentException("Player is not online.");
        return player;
    }

    private void primaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("This LinoAC API operation requires the server main thread.");
        if (!plugin.isEnabled()) throw new IllegalStateException("LinoAC is not enabled.");
    }

    public record CheckDefinition(String id, String family, String category, String description, boolean available) {}

    public record CheckState(CheckDefinition definition, Policy policy, int violations, double buffer) {}

    public record Profile(UUID id, String name, int ping, String brand, String clientVersion, int armSwingsPerSecond,
                          int totalViolations, Map<String, Integer> categoryViolations, Map<String, Integer> violations,
                          Map<String, Double> buffers, boolean frozen, boolean alerts, boolean verbose) {
        public Profile {
            categoryViolations = Map.copyOf(categoryViolations);
            violations = Map.copyOf(violations);
            buffers = Map.copyOf(buffers);
        }
    }

    public record Policy(boolean enabled, double buffer, double decay, int maximumViolations, boolean setback,
                         boolean punishable, List<String> commands, double bufferMultiplier, int minimumSetbackViolations,
                         int alertInterval, int minimumAlertViolations, int maximumPing, double minimumTps,
                         int webhookInterval, boolean broadcastPunishment, int shuffleMinimum, int shuffleInterval,
                         int rotateMinimum, int rotateInterval) {
        public Policy { commands = List.copyOf(commands); }
    }

    public record Statistics(long sessionTotal, OptionalLong persistedTotal, OptionalLong lastSevenCalendarDays) {}
}
