package io.github.lino9999.linoac.config;

import io.github.lino9999.linoac.check.CheckPolicy;
import io.github.lino9999.linoac.check.CheckType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Settings {
    private final YamlConfiguration yaml;
    private final Map<CheckType, CheckPolicy> policies = new EnumMap<>(CheckType.class);
    private final Set<String> excludedWorlds;

    public Settings(JavaPlugin plugin) throws Exception {
        yaml = new YamlConfiguration();
        yaml.load(new File(plugin.getDataFolder(), "config.yml"));
        if (!yaml.isSet("settings.cancel-hits-cancel-event") && yaml.getBoolean("settings.cancel-hits")) yaml.set("settings.cancel-hits-cancel-event", true);
        boolean migrating = yaml.getInt("config-version", 1) < 2;
        Set<String> legacyKeys = new java.util.HashSet<>();
        var resource = java.util.Objects.requireNonNull(plugin.getResource("config.yml"));
        YamlConfiguration defaults;
        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) { defaults = YamlConfiguration.loadConfiguration(reader); }
        if (migrating) {
            var old = yaml.getConfigurationSection("checks");
            if (old != null) for (String key : old.getKeys(false)) {
                var destination = CheckType.find(key);
                if (destination.isPresent() && !key.equals(destination.get().id())) {
                    legacyKeys.add(key);
                    var section = old.getConfigurationSection(key);
                    if (section != null) section.getValues(true).forEach((child, value) -> {
                        if (!(value instanceof org.bukkit.configuration.ConfigurationSection)) yaml.set("checks." + destination.get().id() + "." + child, value);
                    });
                }
            }
        }
        yaml.setDefaults(defaults);
        if (!yaml.isSet("checks.autoclicker-a.max-cps") && yaml.isSet("limits.maximum-cps")) {
            yaml.set("checks.autoclicker-a.max-cps", yaml.get("limits.maximum-cps"));
        }
        if (!yaml.isSet("checks.fastplace-a.max-blocks") && yaml.isSet("limits.maximum-placements")) {
            yaml.set("checks.fastplace-a.max-blocks", yaml.get("limits.maximum-placements"));
        }
        require("checks.fastplace-a.max-blocks", 1, 1000);
        require("checks.timer-a.max-speed", 1, 100);
        require("checks.timer-d.max-balance", 0, 600000);
        require("checks.reach-a.max-reach", 0, 8);
        require("checks.hitbox-a.max-angle", 0, Math.PI);
        require("checks.speed-b.min-difference", 0, 100);
        excludedWorlds = Set.copyOf(yaml.getStringList("settings.excluded-worlds"));
        require("settings.minimum-tps", 1, 20);
        require("settings.maximum-ping", 0, 100000);
        require("settings.join-grace-ticks", 0, 12000);
        require("settings.join-check-wait-time", 0, 600000);
        require("settings.min-ticks-existed", 0, 12000);
        require("settings.flight-cooldown", 0, 12000);
        require("settings.violation-reset-seconds", 1, 86400);
        require("settings.alert-cooldown-ms", 100, 60000);
        require("settings.log-retention-days", 1, 365);
        require("settings.recent-log-limit", 10, 1000);
        require("wave.interval-seconds", 1, 86400);
        require("wave.entry-delay-ticks", 1, 72000);
        require("logs.maximum-file-mib", .001, 1024);
        require("alerts.maximum-violations", 0, 1000000);
        String proxyChannel = text("proxy.channel");
        if (proxyChannel.length() > org.bukkit.plugin.messaging.Messenger.MAX_CHANNEL_SIZE
                || !proxyChannel.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || proxyChannel.startsWith("minecraft:")) throw new IllegalArgumentException("Invalid proxy channel.");
        if (text("proxy.alert-separator").isEmpty()) throw new IllegalArgumentException("Proxy alert separator cannot be empty.");
        for (CheckType type : CheckType.values()) {
            String path = "checks." + type.id();
            require(policyPath(path, "buffer"), 1, 1000);
            require(policyPath(path, "decay"), 0, 100);
            require(policyPath(path, "max-violations"), 1, 100000);
            require(policyPath(path, "buffer-multiplier"), 0, 1);
            require(policyPath(path, "minimum-setback-violations"), 1, 100000);
            require(policyPath(path, "alert-interval"), 1, 100000);
            require(policyPath(path, "minimum-alert-violations"), 0, 100000);
            require(policyPath(path, "maximum-ping"), 0, 100000);
            require(policyPath(path, "minimum-tps"), 1, 20);
            require(policyPath(path, "webhook-interval"), 1, 100000);
            require(policyPath(path, "shuffle-minimum"), 0, 100000);
            require(policyPath(path, "shuffle-interval"), 1, 100000);
            require(policyPath(path, "rotate-minimum"), 0, 100000);
            require(policyPath(path, "rotate-interval"), 1, 100000);
            policies.put(type, new CheckPolicy(type.available() && yaml.getBoolean(policyPath(path, "enabled")), yaml.getDouble(policyPath(path, "buffer")),
                    yaml.getDouble(policyPath(path, "decay")), yaml.getInt(policyPath(path, "max-violations")),
                    yaml.getBoolean(policyPath(path, "setback")), yaml.getBoolean(policyPath(path, "punishable")),
                    List.copyOf(yaml.getStringList(policyPath(path, "commands"))), yaml.getDouble(policyPath(path, "buffer-multiplier")),
                    yaml.getInt(policyPath(path, "minimum-setback-violations")), yaml.getInt(policyPath(path, "alert-interval")),
                    yaml.getInt(policyPath(path, "minimum-alert-violations")), yaml.getInt(policyPath(path, "maximum-ping")), yaml.getDouble(policyPath(path, "minimum-tps")),
                    yaml.getInt(policyPath(path, "webhook-interval")), yaml.getBoolean(policyPath(path, "broadcast-punishment")), yaml.getInt(policyPath(path, "shuffle-minimum")),
                    yaml.getInt(policyPath(path, "shuffle-interval")), yaml.getInt(policyPath(path, "rotate-minimum")), yaml.getInt(policyPath(path, "rotate-interval"))));
        }
        require("limits.reach-margin", 0, 6);
        require("limits.maximum-cps", 5, 200);
        require("checks.autoclicker-a.max-cps", 5, 200);
        require("checks.autoclicker-t.min-kurtosis", -1000000, 1000000);
        require("limits.maximum-placements", 5, 200);
        require("limits.maximum-packets", 50, 10000);
        require("connection.maximum-pending", 10, 1000);
        require("integrations.entity-cram.maximum-entities", 1, 1000);
        require("integrations.entity-cram.grace-ticks", 1, 1200);
        require("connection.timeout-seconds", 5, 300);
        require("connection.maximum-ping", 100, 120000);
        require("settings.statistics-interval-seconds", 60, 604800);
        require("settings.cancel-hits-ms", 0, 60000);
        require("settings.punishment-delay-ticks", 0, 72000);
        require("settings.automatic-punishment-cooldown-ms", 0, 86400000);
        require("ghost-blocks.minimum-ticks", 1, 1200);
        require("limits.bridge-placements", 1, 200);
        require("limits.minimum-break-difference", -10000, 0);
        require("limits.elytra-speed", .1, 100);
        for (String group : List.of("combat", "movement", "player", "autoclicker", "total", "scaffold")) require("limits.improbable-" + group, 0, 1000000);
        if (migrating) {
            legacyKeys.forEach(key -> yaml.set("checks." + key, null));
            yaml.options().copyDefaults(true);
            yaml.set("config-version", 2);
            var path = new File(plugin.getDataFolder(), "config.yml").toPath();
            var backup = path.resolveSibling("config-v1-backup.yml");
            if (!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(path, backup);
            var temporary = path.resolveSibling("config.yml.tmp");
            java.nio.file.Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            try { java.nio.file.Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { java.nio.file.Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        }
    }

    private String policyPath(String path, String key) {
        String specific = path + "." + key;
        return yaml.contains(specific) ? specific : "checks.defaults." + key;
    }

    private void require(String path, double min, double max) {
        Object raw = yaml.get(path);
        double value = raw instanceof Number number ? number.doubleValue() : Double.NaN;
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Invalid " + path + ": expected " + min + " to " + max);
        }
    }

    public CheckPolicy policy(CheckType type) { return policies.get(type); }
    public double number(String path) { return yaml.getDouble(path); }
    public int integer(String path) { return yaml.getInt(path); }
    public boolean enabled(String path) { return yaml.getBoolean(path); }
    public String text(String path) { return java.util.Objects.requireNonNullElse(yaml.getString(path), ""); }
    public List<String> list(String path) { return yaml.getStringList(path); }
    public Set<String> excludedWorlds() { return excludedWorlds; }
}
