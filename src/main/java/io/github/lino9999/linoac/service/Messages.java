package io.github.lino9999.linoac.service;

import io.github.lino9999.linoac.LinoAC;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Messages {
    private final LinoAC plugin;

    public Messages(LinoAC plugin) { this.plugin = plugin; }
    public String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
    public void send(CommandSender sender, String value) { sender.sendMessage(color(plugin.settings().text("messages.prefix") + " " + value)); }
    public void key(CommandSender sender, String key) { send(sender, plugin.settings().text("messages." + key)); }
}
