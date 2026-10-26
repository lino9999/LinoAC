package io.github.lino9999.linoac.listener;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.player.PlayerData;
import io.github.lino9999.linoac.util.Environment;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.collector.SourceEventState.Age;
import io.github.lino9999.linoac.detection.collector.SourceEventState.Tick;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class GameListener implements Listener {
    private final LinoAC plugin;
    private final java.util.Map<java.util.UUID, BlockBreakEvent> activeBedBreaks = new java.util.HashMap<>();

    public GameListener(LinoAC plugin) { this.plugin = plugin; }

    @EventHandler public void join(PlayerJoinEvent event) { plugin.join(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) { plugin.quit(event.getPlayer()); }
    @EventHandler public void respawn(PlayerRespawnEvent event) { plugin.exempt(plugin.data(event.getPlayer()), 100); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void teleport(PlayerTeleportEvent event) {
        PlayerData data = plugin.data(event.getPlayer());
        plugin.exempt(data, 60);
        data.previous = null;
        data.safe = null;
        data.history.clear();
        if (event.getTo() != null) plugin.collector().teleport(event.getPlayer(), event.getTo(), event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN && event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL && event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) plugin.collector().nonPearlTeleport(event.getPlayer());
        else if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) plugin.collector().enderPearlTeleport(event.getPlayer());
    }
    @EventHandler(ignoreCancelled = true) public void velocity(PlayerVelocityEvent event) {
        plugin.exemptMovement(plugin.data(event.getPlayer()), 80);
    }
    @EventHandler(ignoreCancelled = true) public void damage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) plugin.exemptMovement(plugin.data(player), 40);
    }
    @EventHandler(ignoreCancelled = true) public void resurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) plugin.exempt(plugin.data(player), 60);
    }
    @EventHandler(ignoreCancelled = true) public void mode(PlayerGameModeChangeEvent event) { plugin.exemptMovement(plugin.data(event.getPlayer()), 60); }
    @EventHandler(ignoreCancelled = true) public void flight(PlayerToggleFlightEvent event) { plugin.exemptMovement(plugin.data(event.getPlayer()), 60); }
    @EventHandler public void riptide(PlayerRiptideEvent event) { plugin.exemptMovement(plugin.data(event.getPlayer()), 100); }
    @EventHandler(ignoreCancelled = true) public void move(PlayerMoveEvent event) {
        PlayerData data = plugin.data(event.getPlayer());
        if (data.frozen && event.getTo() != null && (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            var location = event.getFrom().clone();
            location.setYaw(event.getTo().getYaw());
            location.setPitch(event.getTo().getPitch());
            event.setTo(location);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void frozenAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (plugin.data(player).frozen) event.setCancelled(true);
        else if (plugin.actions().shouldCancelHit(player)) {
            event.setDamage(0);
            if (plugin.settings().enabled("settings.cancel-hits-cancel-event")) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void trackServerMove(PlayerMoveEvent event) {
        if (event.getTo() != null && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            if (event.isCancelled()) plugin.collector().sourceEvent(event.getPlayer(), Age.CANCELLED_MOVE);
            plugin.serverMoved(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR) public void clickPlacement(BlockPlaceEvent event) {
        plugin.collector().clickContext(event.getPlayer()).placement();
        plugin.collector().placementEvent(event.getPlayer(), event.getBlockPlaced().getType().name(), event.isCancelled());
    }

    @EventHandler(priority = EventPriority.MONITOR) public void sourceDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && !player.hasMetadata("NPC") && !player.hasMetadata("npc")) plugin.collector().sourceDamage(player, event.getDamager().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR) public void sourceDamageCause(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && !player.hasMetadata("NPC") && !player.hasMetadata("npc")) plugin.collector().sourceDamageCause(player, event.getCause().name());
    }

    @EventHandler(priority = EventPriority.LOWEST) public void clickBreak(BlockBreakEvent event) {
        plugin.collector().clickContext(event.getPlayer()).blockBreak(plugin.tick(), event.getBlock().getX(), event.getBlock().getZ(), event.isCancelled());
        plugin.collector().sourceEvent(event.getPlayer(), Age.BLOCK_BREAK);
        if (!io.github.lino9999.linoac.detection.world.GhostHandContext.bed(event.getBlock().getType().name())) return;
        java.util.UUID id = event.getPlayer().getUniqueId();
        BlockBreakEvent previous = activeBedBreaks.put(id, event);
        try { plugin.collector().event(event.getPlayer(), Observation.Kind.BREAK, coordinates(event.getBlock()), java.util.Set.of("ghosthand-callback")); }
        finally { if (previous == null) activeBedBreaks.remove(id); else activeBedBreaks.put(id, previous); }
    }

    @EventHandler(priority = EventPriority.MONITOR) public void acceptedBedViolation(io.github.lino9999.linoac.api.LinoPostViolationEvent event) {
        if (event.getCheck() != CheckType.GHOSTHAND_A || !plugin.settings().enabled("checks.ghosthand-a.cancel")) return;
        BlockBreakEvent active = activeBedBreaks.get(event.getPlayerId());
        if (active != null) active.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR) public void clickInteraction(PlayerInteractEvent event) {
        var action = event.getAction();
        if (action != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR) return;
        plugin.collector().clickContext(event.getPlayer()).blockInteraction(plugin.tick(), System.currentTimeMillis(),
                action != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK,
                event.getItem() == null ? null : event.getItem().getType().name(),
                event.getClickedBlock() == null ? null : event.getClickedBlock().getType().name());
        if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && event.getItem() != null && event.getItem().getType().name().contains("BUCKET")) {
            plugin.collector().bucketInteraction(event.getPlayer());
            if (event.getItem().getType().name().contains("_BUCKET") && event.getClickedBlock() != null && !event.getClickedBlock().getType().isAir()
                    && event.getClickedBlock().getLocation().distance(event.getPlayer().getLocation()) < 2.25) plugin.collector().sourceEvent(event.getPlayer(), Age.EMPTIED_BUCKET);
        }
        if (action == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK && !event.isCancelled() && event.getClickedBlock() != null) plugin.collector().startMining(event.getPlayer(), event.getClickedBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR) public void clickProjectile(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player
                && (event.getEntity() instanceof org.bukkit.entity.Snowball || event.getEntity() instanceof org.bukkit.entity.Egg
                || event.getEntity() instanceof org.bukkit.entity.ThrownExpBottle)) plugin.collector().clickContext(player).projectileLaunch(plugin.tick());
        if (event.getEntity() instanceof org.bukkit.entity.Arrow && event.getEntity().getShooter() instanceof Player player) {
            plugin.collector().event(player, Observation.Kind.RELEASE, java.util.Map.of("projectile-speed", event.getEntity().getVelocity().length()),
                    java.util.Set.of("bow-shot", "source-arrow-launch"));
        }
    }

    @EventHandler public void clickFishing(PlayerFishEvent event) { plugin.collector().clickContext(event.getPlayer()).fishing(plugin.tick()); }
    @EventHandler(priority = EventPriority.MONITOR) public void caughtByHook(PlayerFishEvent event) {
        if (event.getCaught() instanceof Player caught) plugin.collector().sourceEvent(caught, Age.FISHING_ROD);
    }
    @EventHandler(priority = EventPriority.MONITOR) public void consumed(PlayerItemConsumeEvent event) {
        if (event.getItem().getType().name().equals("CHORUS_FRUIT")) plugin.collector().sourceEvent(event.getPlayer(), Age.CHORUS_FRUIT);
    }
    @EventHandler(priority = EventPriority.MONITOR) public void bedEntered(PlayerBedEnterEvent event) { plugin.collector().sourceEvent(event.getPlayer(), Tick.BED_ENTER); }
    @EventHandler(priority = EventPriority.MONITOR) public void bedLeft(PlayerBedLeaveEvent event) { plugin.collector().sourceEvent(event.getPlayer(), Tick.BED_LEAVE); }

    @EventHandler public void clickEntityInteraction(PlayerInteractEntityEvent event) {
        plugin.collector().clickContext(event.getPlayer()).entityInteraction(System.currentTimeMillis(), event.getRightClicked().getType().name());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void acceptedAttack(EntityDamageByEntityEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && event.getDamager() instanceof Player player
                && event.getEntity() instanceof Player) plugin.collector().acceptedAttack(player);
    }

    @EventHandler public void death(PlayerDeathEvent event) { plugin.collector().died(event.getEntity()); }
    @EventHandler public void changedWorld(PlayerChangedWorldEvent event) { plugin.collector().changedWorld(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR) public void place(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.data(player);
        if (data.frozen) return;
        var values = coordinates(event.getBlockPlaced());
        values.put("against-x", (double) event.getBlockAgainst().getX());
        values.put("against-y", (double) event.getBlockAgainst().getY());
        values.put("against-z", (double) event.getBlockAgainst().getZ());
        values.put("block-face", (double) face(event.getBlockAgainst().getFace(event.getBlockPlaced())));
        java.util.Set<String> states = new java.util.HashSet<>();
        if (!event.isCancelled()) states.add("block-accepted");
        else states.add("block-cancelled");
        if (event.getBlockPlaced().getType().isSolid()) states.add("placed-solid");
        String material = event.getBlockPlaced().getType().name();
        if (material.contains("LILY") || material.contains("SCAFFOLD") || material.contains("SIGN")) states.add("placement-exempt");
        plugin.collector().event(player, Observation.Kind.PLACE, values, states);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void freezePlace(BlockPlaceEvent event) {
        if (plugin.data(event.getPlayer()).frozen) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void breakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.data(player);
        if (data.frozen) return;
        var values = coordinates(event.getBlock());
        plugin.collector().event(player, Observation.Kind.BREAK, values, java.util.Set.of("block-accepted", "fastbreak-callback"));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void freezeBreak(BlockBreakEvent event) {
        if (plugin.data(event.getPlayer()).frozen) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true) public void inventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlayerData data = plugin.data(player);
        if (data.frozen) { event.setCancelled(true); return; }
        data.clicks.add(System.nanoTime());
    }

    @EventHandler public void interact(PlayerInteractEvent event) {
        PlayerData data = plugin.data(event.getPlayer());
        if (data.frozen) { event.setCancelled(true); return; }
        if (event.getItem() == null || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        String type = event.getItem().getType().name();
        if (event.getClickedBlock() != null) {
            var values = coordinates(event.getClickedBlock()); values.put("block-face", (double) face(event.getBlockFace()));
            java.util.Set<String> states = new java.util.HashSet<>(); states.add("block-interaction");
            if (event.getClickedBlock().getType().isSolid()) states.add("clicked-solid");
            if (event.getItem().getType().isSolid()) states.add("held-solid");
            if (event.getClickedBlock().equals(event.getPlayer().getLocation().subtract(0, 1, 0).getBlock())) states.add("clicked-below-feet");
            plugin.collector().event(event.getPlayer(), Observation.Kind.USE, values, states);
        }
    }

    private java.util.Map<String, Double> coordinates(Block block) {
        java.util.Map<String, Double> values = new java.util.HashMap<>();
        values.put("block-x", (double) block.getX()); values.put("block-y", (double) block.getY()); values.put("block-z", (double) block.getZ());
        return values;
    }

    private int face(BlockFace face) {
        if (face == null) return -1;
        return switch (face) { case DOWN -> 0; case UP -> 1; case NORTH -> 2; case SOUTH -> 3; case WEST -> 4; case EAST -> 5; case SELF -> 6; default -> -1; };
    }

}
