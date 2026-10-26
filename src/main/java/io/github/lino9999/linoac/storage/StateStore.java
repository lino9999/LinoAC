package io.github.lino9999.linoac.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class StateStore {
    private final Path path;
    private final YamlConfiguration data = new YamlConfiguration();
    private final Map<UUID, String> wave = new LinkedHashMap<>();

    public StateStore(JavaPlugin plugin) throws Exception {
        path = plugin.getDataFolder().toPath().resolve("state.yml");
        if (Files.exists(path)) data.load(path.toFile());
        var section = data.getConfigurationSection("wave");
        if (section != null) for (String key : section.getKeys(false)) {
            UUID id = UUID.fromString(key);
            String name = section.getString(key, "");
            if (name.matches("[A-Za-z0-9_]{1,16}")) wave.put(id, name);
        }
    }

    public boolean alerts(UUID id, boolean fallback) { return data.getBoolean("alerts." + id, fallback); }
    public void setAlerts(UUID id, boolean enabled) { data.set("alerts." + id, enabled); }
    public Map<UUID, String> wave() { return wave; }

    public void save() throws IOException {
        data.set("wave", null);
        wave.forEach((id, name) -> data.set("wave." + id, name));
        Path temporary = path.resolveSibling("state.yml.tmp");
        Files.writeString(temporary, data.saveToString());
        try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
    }
}
