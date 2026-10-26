package io.github.lino9999.linoac.service;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.api.LinoGhostBlockEvent;
import io.github.lino9999.linoac.util.Environment;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostBlockRepair {
    private final LinoAC plugin;
    private final Map<UUID, Integer> mismatches = new HashMap<>();
    private final Map<UUID, Long> repaired = new HashMap<>();

    public GhostBlockRepair(LinoAC plugin) { this.plugin = plugin; }

    public void tick(Player player) {
        var data = plugin.data(player);
        if (!plugin.settings().enabled("ghost-blocks.enabled") || player.isDead() || plugin.tick() < data.graceUntil || !plugin.violations().eligible(player, data)
                || plugin.tick() < data.movementGraceUntil || player.getAllowFlight() || player.isGliding()
                || player.isInsideVehicle() || player.isInWater() || player.isRiptiding()
                || !plugin.collector().clientGround(player) || Environment.supported(player)) {
            mismatches.remove(player.getUniqueId());
            return;
        }
        int count = mismatches.merge(player.getUniqueId(), 1, Integer::sum);
        if (count < plugin.settings().integer("ghost-blocks.minimum-ticks")
                || plugin.tick() - repaired.getOrDefault(player.getUniqueId(), -100L) < 100) return;
        Location location = player.getLocation();
        if (!loaded(location)) return;
        var event = new LinoGhostBlockEvent(player.getUniqueId(), player.getName(), location);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = -2; y <= 2; y++) {
            Location blockLocation = location.clone().add(x, y, z);
            if (!loaded(blockLocation)) continue;
            var block = blockLocation.getBlock();
            player.sendBlockChange(block.getLocation(), block.getBlockData());
        }
        plugin.collector().recordGhostSetback(player);
        repaired.put(player.getUniqueId(), plugin.tick());
        mismatches.remove(player.getUniqueId());
        if (plugin.settings().enabled("ghost-blocks.setback")) plugin.actions().applySetback(player, data.safe, null);
    }

    private boolean loaded(Location location) {
        var world = location.getWorld();
        return world != null && location.getBlockY() >= world.getMinHeight() && location.getBlockY() < world.getMaxHeight()
                && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public void remove(UUID id) { mismatches.remove(id); repaired.remove(id); }
    public void clear() { mismatches.clear(); repaired.clear(); }
}
