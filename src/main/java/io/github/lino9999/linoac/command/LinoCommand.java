package io.github.lino9999.linoac.command;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public final class LinoCommand implements CommandExecutor, TabCompleter, Listener {
    private final LinoAC plugin;
    private final Map<String, String> permissions = Map.ofEntries(
            Map.entry("help", "status"), Map.entry("status", "status"), Map.entry("alerts", "alerts"),
            Map.entry("verbose", "verbose"), Map.entry("profile", "profile"), Map.entry("violations", "profile"),
            Map.entry("cps", "profile"), Map.entry("connection", "profile"), Map.entry("top", "profile"),
            Map.entry("checks", "status"), Map.entry("describe", "status"), Map.entry("menu", "status"),
            Map.entry("reload", "manage"), Map.entry("enable", "manage"), Map.entry("disable", "manage"),
            Map.entry("reset", "manage"), Map.entry("freeze", "freeze"), Map.entry("ban", "punish"),
            Map.entry("wave", "wave"), Map.entry("logs", "logs"), Map.entry("punishlogs", "logs"),
            Map.entry("teleport", "teleport"), Map.entry("knockback", "test"), Map.entry("rotate", "test"),
            Map.entry("shuffle", "test"), Map.entry("testalert", "test"));

    public LinoCommand(LinoAC plugin) { this.plugin = plugin; }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] original) {
        String[] args = shortcut(command.getName(), original);
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        String permission = permissions.get(sub);
        if (permission == null) { plugin.messages().key(sender, "unknown-command"); return true; }
        if (!sender.hasPermission("linoac." + permission)) { plugin.messages().key(sender, "no-permission"); return true; }
        try {
            switch (sub) {
                case "help" -> help(sender);
                case "status" -> plugin.messages().send(sender, "&fLinoAC " + plugin.getDescription().getVersion() + " &7| TPS: " + String.format(Locale.ROOT, "%.2f", plugin.tps())
                        + " | Players: " + Bukkit.getOnlinePlayers().size() + " | Checks: " + plugin.engine().checkIds().size() + " | Catalog: " + CheckType.values().length + " | Auto punishment: " + plugin.settings().enabled("settings.automatic-punishments"));
                case "alerts", "verbose" -> toggle(sender, sub);
                case "profile", "violations", "cps", "connection" -> profile(sender, target(args, 1), sub);
                case "top" -> top(sender, args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "total");
                case "checks" -> { for (CheckType type : CheckType.values()) plugin.messages().send(sender, (plugin.settings().policy(type).enabled() ? "&a" : "&c") + type.id() + " &7[" + type.category() + "]" + (type.available() ? "" : " &8(unavailable upstream slot)")); }
                case "describe" -> { CheckType type = check(args, 1); plugin.messages().send(sender, "&f" + type.id() + " &7" + type.description()); }
                case "menu" -> menu(requirePlayer(sender), args.length > 1 ? Integer.parseInt(args[1]) - 1 : 0);
                case "reload" -> { plugin.reloadSettings(); plugin.messages().send(sender, "&aConfiguration reloaded."); }
                case "enable", "disable" -> configure(sender, check(args, 1), sub.equals("enable"));
                case "reset" -> {
                    boolean reset = args.length > 1 && args[1].equalsIgnoreCase("all")
                            ? plugin.actions().resetAll(sender.getName(), false) : plugin.actions().reset(target(args, 1), sender.getName(), false);
                    plugin.messages().send(sender, reset ? "&aViolations reset." : "&eReset cancelled by an API listener.");
                }
                case "freeze" -> {
                    Player player = target(args, 1);
                    PlayerData data = plugin.data(player);
                    if (plugin.actions().setFrozen(player, !data.frozen, sender.getName())) plugin.messages().send(sender, "&f" + player.getName() + " &7frozen: " + data.frozen);
                    else plugin.messages().send(sender, "&eFreeze change cancelled by an API listener.");
                }
                case "ban" -> {
                    Player player = target(args, 1);
                    boolean result = plugin.punishments().execute(player.getUniqueId(), player.getName(), "staff", plugin.settings().list("punishments.manual-commands"));
                    plugin.messages().send(sender, result ? "&aPunishment commands dispatched." : "&eNo punishment command was dispatched.");
                }
                case "wave" -> wave(sender, args);
                case "logs", "punishlogs" -> logs(sender, args, sub);
                case "teleport" -> {
                    Player staff = requirePlayer(sender);
                    Player target = target(args, 1);
                    staff.teleport(target.getLocation());
                    plugin.messages().send(sender, "&aTeleported to " + target.getName() + ".");
                }
                case "knockback" -> {
                    Player player = target(args, 1);
                    player.setVelocity(new Vector(.35, .4, .35));
                    plugin.exemptMovement(plugin.data(player), 100);
                    plugin.messages().send(sender, "&aKnockback test sent to " + player.getName() + ".");
                }
                case "rotate" -> {
                    Player player = target(args, 1);
                    plugin.actions().rotate(player);
                    plugin.messages().send(sender, "&aRotated " + player.getName() + ".");
                }
                case "shuffle" -> {
                    Player player = target(args, 1);
                    plugin.actions().shuffle(player);
                    plugin.messages().send(sender, "&aShuffled hotbar for " + player.getName() + ".");
                }
                case "testalert" -> {
                    Player player = args.length > 1 ? target(args, 1) : requirePlayer(sender);
                    plugin.violations().alert(player, CheckType.BAD_PACKETS, 0, "Staff test alert; no violation recorded.");
                    plugin.webhooks().sendTest("alerts", "[LinoAC] Staff alert integration test for " + player.getName() + "; no violation recorded.");
                    plugin.webhooks().sendTest("punishments", "[LinoAC] Staff punishment integration test for " + player.getName() + "; no punishment executed.");
                    plugin.messages().send(sender, "&aTest alert sent.");
                }
                default -> plugin.messages().key(sender, "unknown-command");
            }
        } catch (IllegalArgumentException e) { plugin.messages().send(sender, "&c" + e.getMessage()); }
        catch (Exception e) {
            plugin.messages().send(sender, "&cOperation failed: " + e.getMessage());
            plugin.getLogger().warning("Command " + sub + " failed: " + e.getMessage());
        }
        return true;
    }

    private void help(CommandSender sender) {
        plugin.messages().send(sender, "&fLinoAC commands &7(/linoac, /lac, /linoanticheat)");
        for (String sub : List.of("status", "alerts", "verbose", "profile", "violations", "cps", "connection", "top", "checks", "describe", "menu", "reload", "enable", "disable", "reset", "freeze", "ban", "wave", "logs", "punishlogs", "teleport", "knockback", "rotate", "shuffle", "testalert")) {
            if (sender.hasPermission("linoac." + permissions.get(sub))) plugin.messages().send(sender, "&a/lac " + sub + " &7" + syntax(sub));
        }
    }

    private String syntax(String sub) {
        return switch (sub) {
            case "profile", "violations", "cps", "connection", "freeze", "ban", "teleport", "knockback", "rotate", "shuffle" -> "<player>";
            case "enable", "disable", "describe" -> "<check>";
            case "reset" -> "<player|all>";
            case "wave" -> "<add|remove|list|execute> [player]";
            case "logs", "punishlogs" -> "<player> [page]";
            case "testalert" -> "[player]";
            case "top" -> "[total|combat|movement|player]";
            case "menu" -> "[page]";
            default -> "";
        };
    }

    private void toggle(CommandSender sender, String kind) throws java.io.IOException {
        PlayerData data = plugin.data(requirePlayer(sender));
        if (kind.equals("alerts")) {
            if (!plugin.actions().setAlerts(requirePlayer(sender), !data.alerts)) { plugin.messages().send(sender, "&eAlert change cancelled by an API listener."); return; }
        } else data.verbose = !data.verbose;
        plugin.messages().send(sender, "&7" + kind + ": &f" + (kind.equals("alerts") ? data.alerts : data.verbose));
    }

    private void profile(CommandSender sender, Player player, String kind) {
        PlayerData data = plugin.data(player);
        if (kind.equals("profile") || kind.equals("connection")) plugin.messages().send(sender,
                "&f" + player.getName() + " &7| Ping: " + player.getPing() + "ms | Client: " + data.clientVersion + " | Brand (unverified): " + data.brand + " | Frozen: " + data.frozen);
        var metrics = plugin.collector().snapshot(player);
        if (kind.equals("connection")) {
            plugin.messages().send(sender, "&7Acknowledgement ping: &f" + metric(metrics, "transaction-ping") + "ms &7| Pending: &f" + metric(metrics, "pending-transactions")
                    + " &7| Oldest pending: &f" + metric(metrics, "transaction-age-ms") + "ms &7| Last movement interval: &f" + metric(metrics, "move-interval-ms") + "ms");
        }
        if (kind.equals("profile")) {
            plugin.messages().send(sender, "&7UUID: &f" + data.id + " &7| Movement speed: &f" + metric(metrics, "movement-speed")
                    + " &7| Estimated mouse steps: &f" + metric(metrics, "mouse-step-yaw") + "/" + metric(metrics, "mouse-step-pitch"));
            plugin.messages().send(sender, "&7Category VL: &fcombat=" + categoryViolations(player, "combat") + " movement=" + categoryViolations(player, "movement")
                    + " player=" + categoryViolations(player, "player"));
            plugin.messages().send(sender, "&7Effects: &f" + player.getActivePotionEffects().stream().map(effect -> effect.getType().getKey().getKey() + ":" + (effect.getAmplifier() + 1)).sorted().toList());
        }
        if (kind.equals("profile") || kind.equals("cps")) plugin.messages().send(sender, "&7Eligible swing CPS: &f" + String.format(Locale.ROOT, "%.2f", data.clicksPerSecond));
        if (kind.equals("profile") || kind.equals("violations")) {
            plugin.messages().send(sender, "&7Total VL: &f" + data.totalViolations());
            data.violations.forEach((type, vl) -> plugin.messages().send(sender, "&7" + type.id() + ": &f" + vl));
        }
    }

    private String metric(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null || !Double.isFinite(value) ? "unknown" : String.format(Locale.ROOT, "%.3f", value);
    }

    private int categoryViolations(Player player, String category) {
        return plugin.data(player).violations.entrySet().stream().filter(entry -> category.equals("total") || entry.getKey().category().equals(category)).mapToInt(Map.Entry::getValue).sum();
    }

    private void top(CommandSender sender, String category) {
        if (!List.of("total", "combat", "movement", "player").contains(category)) throw new IllegalArgumentException("Unknown violation category.");
        plugin.messages().send(sender, "&7Top online players by " + category + " violations:");
        Bukkit.getOnlinePlayers().stream().sorted(Comparator.comparingInt((Player player) -> categoryViolations(player, category)).reversed()).limit(10)
                .forEach(player -> plugin.messages().send(sender, "&f" + player.getName() + " &7VL: " + categoryViolations(player, category)));
    }

    private void wave(CommandSender sender, String[] args) throws java.io.IOException {
        if (args.length < 2) throw new IllegalArgumentException("Usage: /lac wave " + syntax("wave"));
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                Player player = target(args, 2);
                plugin.punishments().add(player.getUniqueId(), player.getName());
                plugin.messages().send(sender, "&aAdded " + player.getName() + " to the wave.");
            }
            case "remove" -> {
                if (args.length < 3) throw new IllegalArgumentException("Usage: /lac wave remove <player>");
                UUID id = plugin.store().wave().entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(args[2])).map(Map.Entry::getKey).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Player is not queued."));
                plugin.punishments().remove(id);
                plugin.messages().send(sender, "&aRemoved player from the wave.");
            }
            case "list" -> plugin.messages().send(sender, "&7Queued: &f" + String.join(", ", plugin.store().wave().values()));
            case "execute" -> plugin.messages().send(sender, "&7Wave entries scheduled: &f" + plugin.punishments().startWave());
            default -> throw new IllegalArgumentException("Usage: /lac wave " + syntax("wave"));
        }
    }

    private void logs(CommandSender sender, String[] args, String kind) {
        if (args.length < 2 || !args[1].matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Usage: /lac " + kind + " <player> [page]");
        int page = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        int limit = plugin.settings().integer("settings.recent-log-limit");
        if (page < 1 || page > (limit + 7) / 8) throw new IllegalArgumentException("Page is outside the configured recent-log limit.");
        plugin.audit().read(kind.equals("logs") ? "violations" : "punishments", args[1], page, limit, lines -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (sender instanceof Player player && !player.isOnline()) return;
                if (!sender.hasPermission("linoac.logs")) return;
                plugin.messages().send(sender, "&7" + kind + " for &f" + args[1] + " &7| Page " + page);
                if (lines.isEmpty()) plugin.messages().send(sender, "&7No records found.");
                for (String line : lines) plugin.messages().send(sender, "&7" + line.replace('\t', ' '));
            });
        });
    }

    private void configure(CommandSender sender, CheckType type, boolean enabled) throws Exception {
        if (!type.available()) throw new IllegalArgumentException("This upstream catalog slot has no operative detector: " + type.id());
        if (!plugin.actions().beforeCheckToggle(type, enabled, sender.getName())) { plugin.messages().send(sender, "&eCheck change cancelled by an API listener."); return; }
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        yaml.set("checks." + type.id() + ".enabled", enabled);
        yaml.save(file);
        plugin.reloadSettings();
        plugin.messages().send(sender, "&7" + type.id() + " &fenabled: " + enabled);
    }

    private Player target(String[] args, int index) {
        if (args.length <= index) throw new IllegalArgumentException("Specify an online player. See /lac help.");
        Player player = Bukkit.getPlayerExact(args[index]);
        if (player == null) throw new IllegalArgumentException("Player is not online.");
        return player;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) throw new IllegalArgumentException("This command requires a player.");
        return player;
    }

    private CheckType check(String[] args, int index) {
        if (args.length <= index) throw new IllegalArgumentException("Specify a check ID. See /lac checks.");
        return CheckType.find(args[index]).orElseThrow(() -> new IllegalArgumentException("Unknown check ID. See /lac checks."));
    }

    private String[] shortcut(String command, String[] original) {
        String first = switch (command) {
            case "lacalerts" -> "alerts";
            case "lacwave" -> "wave";
            case "laclogs" -> "logs";
            case "lacpunishments" -> "punishlogs";
            default -> "";
        };
        if (first.isEmpty()) return original;
        String[] result = new String[original.length + 1];
        result[0] = first;
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] original) {
        String[] args = shortcut(command.getName(), original);
        List<String> candidates;
        if (args.length <= 1) candidates = permissions.entrySet().stream().filter(e -> sender.hasPermission("linoac." + e.getValue())).map(Map.Entry::getKey).sorted().toList();
        else {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (!sender.hasPermission("linoac." + permissions.getOrDefault(sub, "admin"))) return List.of();
            if (List.of("enable", "disable", "describe").contains(sub) && args.length == 2) candidates = Arrays.stream(CheckType.values()).map(CheckType::id).toList();
            else if (sub.equals("wave") && args.length == 2) candidates = List.of("add", "remove", "list", "execute");
            else if (sub.equals("wave") && args.length == 3 && args[1].equalsIgnoreCase("remove")) candidates = new ArrayList<>(plugin.store().wave().values());
            else if (args.length == 2 && !syntax(sub).isEmpty() || sub.equals("wave") && args.length == 3) candidates = Bukkit.getOnlinePlayers().stream().filter(p -> !(sender instanceof Player viewer) || viewer.canSee(p)).map(Player::getName).toList();
            else candidates = List.of();
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    private void menu(Player player, int page) {
        int pages = (CheckType.values().length + 44) / 45;
        if (page < 0 || page >= pages) throw new IllegalArgumentException("Menu page must be between 1 and " + pages);
        CheckMenu holder = new CheckMenu();
        holder.page = page;
        holder.inventory = Bukkit.createInventory(holder, 54, "LinoAC Checks " + (page + 1) + "/" + pages);
        int end = Math.min(CheckType.values().length, page * 45 + 45);
        for (int index = page * 45; index < end; index++) {
            CheckType type = CheckType.values()[index];
            boolean enabled = plugin.settings().policy(type).enabled();
            ItemStack item = new ItemStack(type.available() ? enabled ? Material.LIME_DYE : Material.GRAY_DYE : Material.BARRIER);
            var meta = item.getItemMeta();
            meta.setDisplayName(plugin.messages().color((enabled ? "&a" : "&c") + type.id()));
            meta.setLore(List.of(type.description(), "Category: " + type.category(), "Enabled: " + enabled,
                    type.available() ? "Click to toggle (linoac.manage)." : "No operative implementation in the upstream artifact."));
            item.setItemMeta(meta);
            holder.inventory.setItem(index - page * 45, item);
        }
        if (page > 0) holder.inventory.setItem(45, new ItemStack(Material.ARROW));
        if (page + 1 < pages) holder.inventory.setItem(53, new ItemStack(Material.ARROW));
        holder.inventory.setItem(49, new ItemStack(Material.OAK_DOOR));
        player.openInventory(holder.inventory);
    }

    @EventHandler public void clickMenu(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CheckMenu holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0 || event.getRawSlot() >= 54 || plugin.data(player).frozen) return;
        if (event.getRawSlot() == 49) { player.closeInventory(); return; }
        if (event.getRawSlot() == 45 && holder.page > 0) { menu(player, holder.page - 1); return; }
        if (event.getRawSlot() == 53 && (holder.page + 1) * 45 < CheckType.values().length) { menu(player, holder.page + 1); return; }
        int index = holder.page * 45 + event.getRawSlot();
        if (event.getRawSlot() >= 45 || index >= CheckType.values().length || !player.hasPermission("linoac.manage")) return;
        CheckType type = CheckType.values()[index];
        try { configure(player, type, !plugin.settings().policy(type).enabled()); menu(player, holder.page); }
        catch (Exception e) { plugin.messages().send(player, "&cUnable to update configuration: " + e.getMessage()); }
    }

    @EventHandler public void dragMenu(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CheckMenu) event.setCancelled(true);
    }

    private static final class CheckMenu implements InventoryHolder {
        private Inventory inventory;
        private int page;
        public Inventory getInventory() { return inventory; }
    }
}
