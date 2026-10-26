package io.github.lino9999.linoac.integration;

import io.github.lino9999.linoac.LinoAC;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

public final class LinoPlaceholders extends PlaceholderExpansion {
    private final LinoAC plugin;

    public LinoPlaceholders(LinoAC plugin) { this.plugin = plugin; }
    public String getIdentifier() { return "linoac"; }
    public String getAuthor() { return "Lino"; }
    public String getVersion() { return plugin.getDescription().getVersion(); }
    public boolean persist() { return true; }

    public String onRequest(OfflinePlayer player, String identifier) {
        return plugin.placeholder(player == null ? null : player.getUniqueId(), identifier.toLowerCase(Locale.ROOT));
    }
}
