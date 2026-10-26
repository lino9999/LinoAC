package io.github.lino9999.linoac.util;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public final class Environment {
    private Environment() {}

    public static double attribute(Player player, String key, double fallback) {
        Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        AttributeInstance instance = attribute == null ? null : player.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }

    public static boolean special(Player player) {
        Location location = player.getLocation();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!location.getWorld().isChunkLoaded((location.getBlockX() + x) >> 4, (location.getBlockZ() + z) >> 4)) return true;
                for (int y = -1; y <= 2; y++) {
                    Block block = location.getWorld().getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                    String name = block.getType().name();
                    if (block.isLiquid() || name.contains("ICE") || name.contains("SLIME") || name.contains("HONEY")
                            || name.contains("WEB") || name.contains("BERRY") || name.contains("POWDER_SNOW")
                            || name.contains("SOUL_SAND") || name.contains("BUBBLE") || name.contains("BED")
                            || name.contains("PISTON")) return true;
                }
            }
        }
        return false;
    }

    public static boolean supported(Player player) {
        Location l = player.getLocation();
        BoundingBox body = player.getBoundingBox();
        BoundingBox feet = new BoundingBox(body.getMinX() + .01, l.getY() - .12, body.getMinZ() + .01,
                body.getMaxX() - .01, l.getY() + .01, body.getMaxZ() - .01);
        for (int x = (int) Math.floor(feet.getMinX()); x <= (int) Math.floor(feet.getMaxX()); x++) {
            for (int z = (int) Math.floor(feet.getMinZ()); z <= (int) Math.floor(feet.getMaxZ()); z++) {
                if (!l.getWorld().isChunkLoaded(x >> 4, z >> 4)) return true;
                for (int y = l.getBlockY() - 2; y <= l.getBlockY(); y++) {
                    Block block = l.getWorld().getBlockAt(x, y, z);
                    if (!block.isPassable() && !block.isLiquid()
                            && block.getCollisionShape().overlaps(feet.clone().shift(-x, -y, -z))) return true;
                }
            }
        }
        return false;
    }
}
