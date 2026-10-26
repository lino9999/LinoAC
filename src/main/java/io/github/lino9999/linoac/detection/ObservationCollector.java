package io.github.lino9999.linoac.detection;

import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.detection.collector.HorizontalVelocityModel;
import io.github.lino9999.linoac.detection.collector.CinematicState;
import io.github.lino9999.linoac.detection.collector.PositionState;
import io.github.lino9999.linoac.detection.collector.ClickContext;
import io.github.lino9999.linoac.detection.collector.SourceEventState;
import io.github.lino9999.linoac.detection.collector.SourceEventState.Age;
import io.github.lino9999.linoac.detection.collector.SourceEventState.Tick;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;
import io.github.lino9999.linoac.packet.WirePacket;
import io.github.lino9999.linoac.player.PlayerData;
import io.github.lino9999.linoac.util.Environment;
import io.github.lino9999.linoac.util.Geometry;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiConsumer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public final class ObservationCollector {
    private static final List<String> ACK_STATE_KEYS = List.of("protocol-flight-allowed", "protocol-movement-speed", "protocol-attack-range", "protocol-levitation-amplifier", "protocol-jump-amplifier", "protocol-conduit-power", "protocol-speed-amplifier", "protocol-jump-strength", "protocol-last-speed-amplifier", "protocol-cached-creative", "protocol-cached-spectator",
            "protocol-slow-falling", "protocol-metadata-gliding", "protocol-gliding-revision", "protocol-metadata-riptide", "protocol-metadata-swimming");
    private static final Set<String> WORLD_POSITION_FACTS = worldPositionFacts();
    private final LinoAC plugin;
    private final BiConsumer<Player, Observation> sink;
    private final Map<UUID, Tracking> tracking = new HashMap<>();
    private final Map<UUID, Long> worldTokens = new HashMap<>();
    private long nextWorldToken;

    private static Set<String> worldPositionFacts() {
        var keys = new HashSet<>(io.github.lino9999.linoac.packet.ServerWorldSnapshot.materialFactKeys());
        keys.addAll(Set.of("source-world-border", "source-nearby-living-contact", "source-nearby-boat-contact", "source-nearby-shulker-contact", "source-nearby-happy-ghast-contact"));
        return Set.copyOf(keys);
    }

    public ObservationCollector(LinoAC plugin, BiConsumer<Player, Observation> sink) {
        this.plugin = plugin;
        this.sink = sink;
    }

    public void remove(UUID id) { tracking.remove(id); }
    public void clear() { tracking.clear(); }
    public void reset(UUID id) { tracking.remove(id); }

    public void connection(Player player, int pending, double oldestAgeMs, boolean tracked) {
        Tracking t = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        t.connectionTracked = tracked;
        if (tracked && pending >= 0 && Double.isFinite(oldestAgeMs) && oldestAgeMs >= 0) {
            t.pendingTransactions = pending;
            t.oldestTransactionMs = oldestAgeMs;
        } else t.connectionTracked = false;
    }

    public boolean clientGround(Player player) {
        Tracking t = tracking.get(player.getUniqueId());
        return t != null && t.clientGround;
    }

    public Map<String, Double> snapshot(Player player) {
        Tracking t = tracking.get(player.getUniqueId());
        if (t == null) return Map.of();
        Map<String, Double> values = new HashMap<>(t.context);
        values.put("source-profile-complete", t.transportComplete ? 1d : 0d);
        values.putAll(t.lastMotion);
        connectionValues(t, values, new HashSet<>());
        values.put("ghost-block-setbacks", (double) t.ghostSetbacks);
        var totals = plugin.data(player).historyTotals.snapshot();
        values.put("cumulative-violations-total", (double) totals.total()); values.put("cumulative-violations-combat", (double) totals.combat());
        values.put("cumulative-violations-movement", (double) totals.movement()); values.put("cumulative-violations-player", (double) totals.player());
        values.put("cumulative-violations-autoclicker", (double) totals.autoClicker()); values.put("cumulative-violations-scaffold", (double) totals.scaffold());
        values.put("cumulative-violations-timer", (double) totals.timer());
        return Map.copyOf(values);
    }

    public void recordGhostSetback(Player player) {
        Tracking t = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        t.ghostSetbacks = Math.min(1_000_000, t.ghostSetbacks + 1);
        t.ghostCorrections = Math.min(1_000_000, t.ghostCorrections + 1);
    }

    public void bucketInteraction(Player player) { tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation())).protocolContext.bucketInteraction(); }

    public void startMining(Player player, Block block) {
        if (player.getGameMode() != GameMode.SURVIVAL || io.github.lino9999.linoac.detection.world.MiningContext.materialExempt(block.getType().name())
                || io.github.lino9999.linoac.detection.world.MiningContext.toolExempt(player.getInventory().getItemInMainHand().getType().name(), player.getInventory().getItemInOffHand().getType().name())) return;
        double speed = block.getBreakSpeed(player);
        if (!Double.isFinite(speed) || speed <= 0) return;
        Tracking state = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        state.mining.start(System.currentTimeMillis(), io.github.lino9999.linoac.detection.world.MiningContext.modernBudgetMillis(speed));
    }

    public io.github.lino9999.linoac.packet.ServerWorldSnapshot serverMoved(Player player, Location destination, List<Entity> nearby) {
        Tracking state = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        state.serverNearbyEntities = nearby.size();
        state.serverNearbyFirework = nearby.stream().anyMatch(entity -> entity instanceof Firework);
        state.serverDestination = destination.clone();
        var types = new io.github.lino9999.linoac.packet.ServerWorldSnapshot.Nearby(nearby.stream().anyMatch(entity -> entity instanceof LivingEntity),
                nearby.stream().anyMatch(entity -> entity.getType().name().contains("BOAT") || entity.getType().name().contains("RAFT")),
                nearby.stream().anyMatch(entity -> entity.getType().name().equals("SHULKER")), nearby.stream().anyMatch(entity -> entity.getType().name().equals("HAPPY_GHAST")));
        state.serverWorld = sampleWorld(player, destination, nearby.size(), ++state.worldGeneration, types);
        return state.serverWorld;
    }

    private io.github.lino9999.linoac.packet.ServerWorldSnapshot sampleWorld(Player player, Location destination, int nearby, long generation, io.github.lino9999.linoac.packet.ServerWorldSnapshot.Nearby types) {
        int centerX = destination.getBlockX(), centerY = destination.getBlockY(), centerZ = destination.getBlockZ();
        if (!player.getWorld().equals(destination.getWorld())) return io.github.lino9999.linoac.packet.ServerWorldSnapshot.unknown(nearby);
        for (int depth = 1; depth <= 3; depth++) if (!loaded(player, centerX, centerY - depth, centerZ)) return io.github.lino9999.linoac.packet.ServerWorldSnapshot.unknown(nearby);
        int air = 0; boolean cobweb = false;
        var cells = new ArrayList<io.github.lino9999.linoac.packet.ServerWorldSnapshot.Cell>(36);
        for (int x = centerX - 1; x <= centerX + 1; x++) for (int y = centerY - 1; y <= centerY + 2; y++) for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            if (!loaded(player, x, y, z)) return io.github.lino9999.linoac.packet.ServerWorldSnapshot.unknown(nearby);
            Material material = player.getWorld().getBlockAt(x, y, z).getType();
            cells.add(new io.github.lino9999.linoac.packet.ServerWorldSnapshot.Cell(material.name(), material.isSolid(), material.isAir()));
            if (material == Material.COBWEB) cobweb = true;
            if (y == centerY - 1 && material.isAir()) air++;
        }
        var border = player.getWorld().getWorldBorder();
        var borderCenter = border.getCenter();
        boolean waterlogged = false;
        for (int y = centerY - 1; y <= centerY + 2; y++) {
            var data = player.getWorld().getBlockAt(centerX, y, centerZ).getBlockData();
            if (data instanceof org.bukkit.block.data.Waterlogged state && state.isWaterlogged()) waterlogged = true;
        }
        return new io.github.lino9999.linoac.packet.ServerWorldSnapshot(nearby, true, cobweb, air, player.getWorld().getBlockAt(centerX, centerY - 2, centerZ).getType().isAir(),
                cells, borderCenter.getX(), borderCenter.getZ(), border.getSize(), generation, waterlogged,
                player.getWorld().getBlockAt(centerX, centerY - 3, centerZ).getType() == Material.SLIME_BLOCK, types);
    }

    public ClickContext clickContext(Player player) {
        if (!org.bukkit.Bukkit.isPrimaryThread()) throw new IllegalStateException("Click context requires the server main thread.");
        return tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation())).clicks;
    }

    public void acceptedAttack(Player player) {
        tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation())).acceptedAttackTick = plugin.tick();
    }

    public void died(Player player) { sourceEvent(player, Age.DEATH); }
    public void changedWorld(Player player) { sourceEvent(player, Age.WORLD_CHANGE); }
    public void sourceEvent(Player player, Age event) { tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation())).events.mark(event); }
    public void sourceEvent(Player player, Tick event) { tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation())).events.mark(event, plugin.tick()); }
    public void placementEvent(Player player, String material, boolean cancelled) {
        var inventory = player.getInventory();
        tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation())).events.placement(material, cancelled,
                inventory.getItemInMainHand().getAmount(), inventory.getHeldItemSlot());
    }

    public void tick(Player player) {
        PlayerData data = plugin.data(player);
        Tracking t = tracking.computeIfAbsent(data.id, ignored -> new Tracking(player.getLocation()));
        if (!t.transportComplete) return;
        Location current = player.getLocation();
        boolean ground = Environment.supported(player);
        t.groundTicks = ground ? t.groundTicks + 1 : 0;
        t.airTicks = ground ? 0 : t.airTicks + 1;
        t.glideTicks = player.isGliding() ? t.glideTicks + 1 : 0;
        Map<String, Double> values = new HashMap<>();
        Set<String> states = new HashSet<>();
        stateValues(player, t, values, states);
        values.put("x", current.getX()); values.put("y", current.getY()); values.put("z", current.getZ());
        values.put("yaw", (double) current.getYaw()); values.put("pitch", (double) current.getPitch());
        values.put("server-dx", current.getX() - t.serverLocation.getX());
        values.put("server-dy", current.getY() - t.serverLocation.getY());
        values.put("server-dz", current.getZ() - t.serverLocation.getZ());
        t.serverHorizontal = Math.hypot(values.get("server-dx"), values.get("server-dz"));
        values.putAll(t.lastMotion);
        values.put("air-ticks", (double) t.airTicks); values.put("ground-ticks", (double) t.groundTicks);
        values.put("glide-ticks", (double) t.glideTicks);
        values.put("food", (double) player.getFoodLevel()); values.put("fall-distance", (double) player.getFallDistance());
        values.put("walk-speed", (double) player.getWalkSpeed());
        values.put("movement-speed", Environment.attribute(player, "movement_speed", .1));
        values.put("jump-strength", Environment.attribute(player, "jump_strength", .42));
        values.put("step-height", Environment.attribute(player, "step_height", .6));
        values.put("gravity", Environment.attribute(player, "gravity", .08));
        values.put("attack-range", Environment.attribute(player, "entity_interaction_range", 3));
        values.put("block-range", Environment.attribute(player, "block_interaction_range", 4.5));
        values.put("ping", (double) player.getPing()); values.put("tps", plugin.tps());
        values.put("tick-scale", plugin.tickScale());
        values.put("speed-effect", (double) effect(player, PotionEffectType.SPEED));
        values.put("jump-effect", (double) effect(player, PotionEffectType.JUMP_BOOST));
        if (values.get("jump-effect") > 0) states.add("jump-effect");
        values.put("levitation-effect", (double) effect(player, PotionEffectType.LEVITATION));
        values.put("vertical-velocity", t.velocity.getY());
        values.put("horizontal-velocity", Math.hypot(t.velocity.getX(), t.velocity.getZ()));
        values.put("velocity-age-ticks", (double) Math.max(0, plugin.tick() - t.velocityTick));
        values.put("attack-age-ticks", (double) Math.max(0, plugin.tick() - t.attackTick));
        values.put("self-id", (double) player.getEntityId());
        values.put("client-ground", t.clientGround ? 1d : 0d); values.put("server-ground", ground ? 1d : 0d);
        values.put("cps-limit", plugin.settings().number("limits.maximum-cps"));
        if (t.inputKnown) {
            values.put("strafe-input", t.strafeInput); values.put("forward-input", t.forwardInput); states.add("movement-input-known");
        }
        connectionValues(t, values, states);
        values.put("ghost-block-setbacks", (double) t.ghostSetbacks);
        values.put("since-teleport", (double) Math.max(0, plugin.tick() - t.teleportTick));
        if (ground) states.add("ground");
        if (t.serverGround) states.add("previous-ground");
        if (t.clientGround) states.add("client-ground");
        if (t.previousClientGround) states.add("client-previous-ground");
        if (player.getAllowFlight() || player.isFlying()) states.add("allow-flight");
        if (player.getAllowFlight() || player.isFlying()) states.add("source-flight");
        if (player.getGameMode() == GameMode.CREATIVE) states.add("source-creative");
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) states.add("creative");
        if (player.getGameMode() == GameMode.SPECTATOR) states.add("spectator");
        if (player.isDead()) states.add("dead");
        if (player.isInWater() || player.isSwimming()) states.add("liquid");
        if (player.isClimbing()) states.add("climbing");
        if (player.isGliding()) states.add("gliding");
        if (player.isRiptiding()) states.add("riptiding");
        if (player.isHandRaised()) states.add("using-item");
        if (player.isBlocking()) states.add("blocking");
        if (player.isSprinting()) states.add("sprinting");
        if (plugin.mechanics().berserk(player.getUniqueId())) states.add("mcmmo-berserk");
        if (player.isSneaking()) states.add("sneaking");
        if (plugin.tick() - t.attackTick < 60) states.add("combat");
        if (plugin.tick() < data.movementGraceUntil) states.add("movement-grace");
        if (t.digging) states.add("digging");
        blockGrace(t, states);
        if (t.actualVelocitySeen && t.velocityConfirmed) { states.add("velocity-confirmed"); states.add("velocity-acknowledged"); }
        protocolStates(player, states);
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) states.add("slow-falling");
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) states.add("levitation");
        var boots = player.getInventory().getBoots();
        var leggings = player.getInventory().getLeggings();
        if (leggings != null && leggings.getEnchantmentLevel(Enchantment.SWIFT_SNEAK) > 0) states.add("swift-sneak");
        values.put("soul-speed-level", boots == null ? 0d : (double) boots.getEnchantmentLevel(Enchantment.SOUL_SPEED));
        values.put("depth-strider-level", boots == null ? 0d : (double) boots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER));
        var weapon = player.getInventory().getItemInMainHand().getType().name();
        if (weapon.contains("SPEAR") || weapon.contains("TRIDENT")) { states.add("extended-weapon"); states.add("extended-melee"); }
        if (weapon.contains("SWORD")) states.add("blocking-weapon");
        environment(player, values, states);
        vehicle(player, t, values, states);
        if (player.isSwimming()) states.add("swimming");
        if (player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) states.add("dolphins-grace");
        if (player.isGliding()) for (Entity nearby : player.getNearbyEntities(4, 4, 4)) {
            if (nearby instanceof Firework firework && player.equals(firework.getAttachedTo())) t.fireworkUntil = plugin.tick() + 40;
        }
        if (plugin.tick() < t.fireworkUntil) states.add("firework-boost");
        boolean physicsKnown = states.contains("support-known") && !states.contains("environment-special") && !states.contains("liquid")
                && !states.contains("allow-flight") && !states.contains("gliding") && !states.contains("vehicle") && !states.contains("riptiding")
                && !states.contains("levitation") && !states.contains("slow-falling") && !states.contains("teleport-exemption") && !states.contains("dead");
        if (physicsKnown && !states.contains("movement-grace")) {
            states.add("physics-known");
            double scale = Math.max(.1, values.get("movement-speed") / .1);
            double previousHorizontal = values.getOrDefault("previous-horizontal", 0d);
            values.put("horizontal-limit", Math.max(.36 * scale + .1, previousHorizontal * .91 + .1));
            if (values.containsKey("previous-dy")) {
                double vertical = Math.max(values.get("jump-strength") + values.get("jump-effect") * .1,
                        Math.abs((values.get("previous-dy") - values.get("gravity")) * .98));
                values.put("vertical-limit", vertical + .15);
            }
        }
        if (physicsKnown) states.add("velocity-physics-known");
        if (t.targetId >= 0 && plugin.tick() - t.attackTick < 60) attack(player, t, t.targetId, values, states);
        t.context = Map.copyOf(values); t.states = Set.copyOf(states);
        data.history.addLast(player.getBoundingBox().clone());
        if (data.history.size() > 20) data.history.removeFirst();
        t.serverLocation = current; t.serverGround = ground;
        t.serverSampleTick = plugin.tick();
        sink.accept(player, new Observation(Observation.Kind.TICK, plugin.tick(), System.nanoTime(), values, states));
    }

    public void accept(Player player, WirePacket packet) {
        Tracking t = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        if (packet.type().equals("ACK_INVALID_IMPULSES")) { t.sourceVelocity.invalidate(); t.transportComplete = false; return; }
        if (packet.type().equals("ACK_RESET_IMPULSES")) {
            t.sourceVelocity = new io.github.lino9999.linoac.detection.combat.VelocityContext();
            t.velocityPrediction = new io.github.lino9999.linoac.detection.combat.VelocityPrediction();
            t.abilities = new io.github.lino9999.linoac.detection.collector.SourceAbilityState();
            return;
        }
        if (packet.type().startsWith("ACK_BEGIN_") || packet.type().startsWith("ACK_END_")) {
            switch (packet.type()) {
                case "ACK_BEGIN_VELOCITY" -> t.sourceVelocity.beginVelocity(packet.values().get("velocity-x"), packet.values().get("velocity-y"), packet.values().get("velocity-z"));
                case "ACK_END_VELOCITY" -> t.sourceVelocity.endVelocity();
                case "ACK_BEGIN_EXPLOSION" -> t.sourceVelocity.beginExplosion(packet.values().get("velocity-x"), packet.values().get("velocity-y"), packet.values().get("velocity-z"));
                case "ACK_END_EXPLOSION" -> t.sourceVelocity.endExplosion();
                default -> throw new IllegalArgumentException("Unknown impulse acknowledgement.");
            }
            return;
        }
        if (!t.transportComplete) {
            if (packet.kind() == Observation.Kind.VELOCITY || packet.kind() == Observation.Kind.SERVER_POSITION) return;
            var minimal = new HashMap<>(packet.values());
            minimal.put("self-id", (double) player.getEntityId());
            minimal.put("protocol-bukkit-spectator", player.getGameMode() == GameMode.SPECTATOR ? 1d : 0d);
            var flags = new HashSet<>(packet.states()); flags.add("source-profile-incomplete");
            sink.accept(player, new Observation(packet.kind(), plugin.tick(), packet.nanos(), minimal, flags));
            return;
        }
        if (packet.type().equals("SERVER_RELATIVE_TARGET")) {
            t.targetHistory.relative(packet.values().get("target-id").intValue(), packet.values().get("target-raw-x"), packet.values().get("target-raw-z"),
                    packet.values().get("target-delta-x"), packet.values().get("target-delta-z"), packet.values().get("history-server-tick").longValue());
            return;
        }
        if (packet.kind() == Observation.Kind.SERVER_POSITION && packet.state("packet:SERVER_POSITION_AND_LOOK")) t.targetHistory.serverPosition();
        if (packet.kind() != Observation.Kind.VELOCITY && packet.kind() != Observation.Kind.SERVER_POSITION) {
            t.acknowledgedState.clear();
            for (String key : ACK_STATE_KEYS) {
                if (packet.values().containsKey(key)) t.acknowledgedState.put(key, packet.values().get(key));
            }
            t.abilities.metadata(t.acknowledgedState.containsKey("protocol-metadata-gliding") ? t.acknowledgedState.get("protocol-metadata-gliding") == 1 : null,
                    t.acknowledgedState.getOrDefault("protocol-gliding-revision", Double.NaN));
        }
        Map<String, Double> values = new HashMap<>(t.context);
        values.putAll(t.lastMotion);
        Set<String> states = new HashSet<>(t.states);
        stateValues(player, t, values, states);
        blockGrace(t, states);
        states.addAll(packet.states());
        values.putAll(packet.values());
        if (t.lastPacketNanos > 0 && packet.nanos() >= t.lastPacketNanos) values.put("packet-interval-ms", (packet.nanos() - t.lastPacketNanos) / 1_000_000d);
        values.put("packets-since-move", (double) t.packetsSinceMove);
        values.put("attack-age-ticks", (double) Math.max(0, plugin.tick() - t.attackTick));
        values.put("client-ground", t.clientGround ? 1d : 0d);
        states.remove("velocity-acknowledged"); states.remove("velocity-confirmed"); states.remove("velocity-model-known");
        if (t.actualVelocitySeen && t.velocityConfirmed) { states.add("velocity-acknowledged"); states.add("velocity-confirmed"); }
        states.remove("teleport-grace");
        if (plugin.tick() < plugin.data(player).movementGraceUntil) states.add("movement-grace"); else states.remove("movement-grace");
        connectionValues(t, values, states);
        values.put("ghost-block-setbacks", (double) t.ghostSetbacks);
        values.put("swing-age-ticks", (double) Math.max(0, plugin.tick() - t.swingTick));
        values.put("attacks-since-swing", (double) t.attacksSinceSwing);
        values.put("swings-since-attack", (double) t.swingsSinceAttack);
        values.put("placements-since-move", (double) t.placementsSinceMove);
        values.put("digs-since-move", (double) t.digsSinceMove);
        values.put("actions-since-move", (double) t.actionsSinceMove);
        values.put("attacks-since-move", (double) t.attacksSinceMove);
        values.put("slot-changes-since-move", (double) t.slotsSinceMove);
        if (t.previousType != null) states.add("previous-packet:" + t.previousType);
        if (t.digging) states.add("digging");
        if (packet.states().contains("nonfinite-position") || packet.states().contains("nonfinite-rotation") || packet.states().contains("outside-world") || packet.states().contains("invalid-pitch")) {
            sink.accept(player, new Observation(packet.states().contains("invalid-pitch") ? Observation.Kind.ROTATION : packet.kind(), plugin.tick(), packet.nanos(), values, states));
            return;
        }
        if (packet.kind() == Observation.Kind.MOVE) {
            if (packet.state("position-packet")) t.sourceVelocity.position(packet.state("client-ground"));
            if (packet.values().keySet().containsAll(Set.of("server-world-generation", "server-world-waterlogged"))) {
                t.cachedWorld.observe(packet.values().get("server-world-generation").longValue(), packet.values().get("server-world-waterlogged") == 1);
            }
            t.totalMoves = Math.min(Integer.MAX_VALUE - 1, t.totalMoves) + 1;
            t.attackMovementAge = Math.min(Integer.MAX_VALUE - 1, t.attackMovementAge) + 1;
            t.events.flying();
            t.bukkitTeleportMoves = Math.min(Integer.MAX_VALUE - 1, t.bukkitTeleportMoves) + 1;
            if (++t.ghostCorrectionMoves >= 100) { t.ghostCorrectionMoves = 0; t.ghostCorrections = 0; }
            if (packet.states().contains("position-packet")) t.mirroredNearbyEntities = packet.values().getOrDefault("mirrored-nearby-entities", -1d).intValue();
            if (packet.state("position-packet") && packet.values().keySet().containsAll(Set.of("x", "y", "z"))) {
                int nearby = packet.values().getOrDefault("server-nearby-entities", -1d).intValue();
                boolean worldKnown = nearby >= 0 && packet.values().getOrDefault("server-world-sample-known", 0d) == 1
                        && packet.values().keySet().containsAll(Set.of("server-world-cobweb", "server-world-air-below", "server-world-two-below-air"));
                var world = worldKnown ? new io.github.lino9999.linoac.packet.ServerWorldSnapshot(nearby, true,
                        packet.values().get("server-world-cobweb") == 1, packet.values().get("server-world-air-below").intValue(), packet.values().get("server-world-two-below-air") == 1)
                        : io.github.lino9999.linoac.packet.ServerWorldSnapshot.unknown(nearby);
                t.positionState.position(packet.values().get("x"), packet.values().get("y"), packet.values().get("z"), world,
                        player.isInsideVehicle() || player.getVehicle() != null, plugin.settings().integer("integrations.entity-cram.maximum-entities"));
                t.positionState.flight(t.acknowledgedState.containsKey("protocol-flight-allowed"), player.getAllowFlight() || t.acknowledgedState.getOrDefault("protocol-flight-allowed", 0d) == 1);
                t.abilities.position(t.acknowledgedState.containsKey("protocol-slow-falling") ? t.acknowledgedState.get("protocol-slow-falling") > 0 : null,
                        t.acknowledgedState.containsKey("protocol-levitation-amplifier") ? t.acknowledgedState.get("protocol-levitation-amplifier") > 0 : null,
                        player.isRiptiding() ? true : t.acknowledgedState.containsKey("protocol-metadata-riptide") ? t.acknowledgedState.get("protocol-metadata-riptide") == 1 : null,
                        player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE), t.cachedChestElytra,
                        t.acknowledgedState.containsKey("protocol-metadata-swimming") ? t.acknowledgedState.get("protocol-metadata-swimming") == 1 : null);
                t.positionGeometry.clear();
                for (String key : WORLD_POSITION_FACTS) {
                    if (packet.values().containsKey(key)) t.positionGeometry.put(key, packet.values().get(key));
                }
                if (plugin.settings().enabled("settings.velocity-world-border")) {
                    var border = player.getWorld().getWorldBorder(); var center = border.getCenter();
                    boolean near = io.github.lino9999.linoac.packet.ServerWorldSnapshot.nearBorder(packet.values().get("x"), packet.values().get("z"), center.getX(), center.getZ(), border.getSize());
                    t.positionGeometry.put("source-world-border", near ? 1d : 0d);
                }
                t.cachedWorld.position(t.positionGeometry.containsKey("source-ceiling-obstructed"), t.positionGeometry.getOrDefault("source-bubble-column", 0d) == 1,
                        t.positionGeometry.getOrDefault("source-slime-below", 0d) == 1, t.positionGeometry.getOrDefault("source-ceiling-obstructed", 0d) == 1);
                boolean soulSpeed = t.cachedBootsSoulSpeed && t.positionGeometry.getOrDefault("source-soul-sand", 0d) == 1;
                t.cachedWorld.contacts(io.github.lino9999.linoac.detection.collector.CachedWorldState.Contacts.from(t.positionGeometry, soulSpeed));
                if (t.positionGeometry.containsKey("source-ceiling-obstructed")) {
                    t.ceilingPositionAge = t.positionGeometry.get("source-ceiling-obstructed") == 1 ? 0 : Math.min(Integer.MAX_VALUE - 1, t.ceilingPositionAge) + 1;
                }
            }
            if (packet.states().contains("position-packet")) t.mirroredNearbyBoats = packet.values().getOrDefault("mirrored-nearby-boats", -1d).intValue();
            if (packet.values().containsKey("teleport-movement-age")) t.teleportMovementAge = packet.values().get("teleport-movement-age").intValue();
            t.serverTeleportMatch = packet.state("server-teleport-match");
            for (String key : List.of("predicted-horizontal-min", "predicted-horizontal-max", "predicted-vertical-min", "predicted-vertical-max", "predicted-projection-min", "predicted-projection-max")) values.remove(key);
            double x = packet.values().getOrDefault("x", t.position.getX());
            double y = packet.values().getOrDefault("y", t.position.getY());
            double z = packet.values().getOrDefault("z", t.position.getZ());
            double yaw = packet.values().getOrDefault("yaw", (double) t.position.getYaw());
            double pitch = packet.values().getOrDefault("pitch", (double) t.position.getPitch());
            double dx = x - t.position.getX(), dy = y - t.position.getY(), dz = z - t.position.getZ();
            t.clicks.movement((long) packet.values().getOrDefault("packet-time-ms", (double) System.currentTimeMillis()).doubleValue(), x, z);
            if (t.serverNearbyFirework) t.clicks.fireworkCollision();
            double deltaYaw = Statistics.wrappedDegrees(yaw - t.position.getYaw());
            double deltaPitch = pitch - t.position.getPitch();
            values.put("x", x); values.put("y", y); values.put("z", z); values.put("yaw", yaw); values.put("pitch", pitch);
            values.put("dx", dx); values.put("dy", dy); values.put("dz", dz); values.put("horizontal", Math.hypot(dx, dz));
            values.put("previous-dx", t.dx); values.put("previous-dy", t.dy); values.put("previous-dz", t.dz);
            values.put("previous-horizontal", Math.hypot(t.dx, t.dz));
            values.put("delta-yaw", deltaYaw); values.put("delta-pitch", deltaPitch);
            values.put("signed-delta-pitch", deltaPitch);
            values.put("previous-delta-yaw", t.deltaYaw); values.put("previous-delta-pitch", t.deltaPitch);
            values.put("yaw-acceleration", Math.abs(deltaYaw) - Math.abs(t.deltaYaw));
            values.put("pitch-acceleration", Math.abs(deltaPitch) - Math.abs(t.deltaPitch));
            if (packet.states().contains("rotation-changed")) {
                t.cinematic.rotate((float) yaw, (float) pitch, (int) plugin.tick());
                t.sensitivitySamples++;
                double yawStep = stableQuantum(t.yawQuanta, Statistics.gcd(deltaYaw, t.deltaYaw));
                double pitchStep = stableQuantum(t.pitchQuanta, Statistics.gcd(deltaPitch, t.deltaPitch));
                if (yawStep > 0 && pitchStep > 0) { values.put("mouse-step-yaw", yawStep); values.put("mouse-step-pitch", pitchStep); }
            }
            values.put("sensitivity-samples", (double) t.sensitivitySamples);
            stateValues(player, t, values, states);
            values.put("client-ground", packet.states().contains("client-ground") ? 1d : 0d);
            double age = Math.max(0, plugin.tick() - t.velocityTick);
            values.put("vertical-velocity", t.velocity.getY()); values.put("horizontal-velocity", Math.hypot(t.velocity.getX(), t.velocity.getZ()));
            values.put("velocity-age-ticks", age);
            double horizontalVelocity = Math.hypot(t.velocity.getX(), t.velocity.getZ());
            values.put("velocity-projection", horizontalVelocity == 0 ? 0 : (dx * t.velocity.getX() + dz * t.velocity.getZ()) / horizontalVelocity);
            velocityModel(t, values, states, yaw);
            if (t.moveNanos > 0 && packet.nanos() >= t.moveNanos) values.put("move-interval-ms", (packet.nanos() - t.moveNanos) / 1_000_000d);
            states.remove("client-ground"); states.remove("client-previous-ground");
            if (packet.states().contains("client-ground")) states.add("client-ground");
            if (t.clientGround) states.add("client-previous-ground");
            Map<String, Double> motion = new HashMap<>();
            for (String key : List.of("x","y","z","yaw","pitch","dx","dy","dz","horizontal","previous-dx","previous-dy","previous-dz","previous-horizontal",
                    "delta-yaw","delta-pitch","signed-delta-pitch","previous-delta-yaw","previous-delta-pitch","yaw-acceleration","pitch-acceleration","sensitivity-samples","mouse-step-yaw","mouse-step-pitch","move-interval-ms")) {
                if (values.containsKey(key)) motion.put(key, values.get(key));
            }
            t.lastMotion = Map.copyOf(motion);
            if (t.targetId >= 0 && plugin.tick() - t.attackTick < 60) targetMotion(player, t, values, states);
            t.position = new Location(player.getWorld(), x, y, z, (float) yaw, (float) pitch);
            t.dx = dx; t.dy = dy; t.dz = dz; t.deltaYaw = deltaYaw; t.deltaPitch = deltaPitch;
            t.sourcePreviousPreviousGround = t.previousClientGround;
            t.previousClientGround = t.clientGround; t.clientGround = packet.states().contains("client-ground");
            t.moveNanos = packet.nanos();
            var currentBoots = player.getInventory().getBoots();
            t.cachedBootsSoulSpeed = currentBoots != null && currentBoots.getEnchantmentLevel(Enchantment.SOUL_SPEED) > 0;
            var currentChest = player.getInventory().getChestplate();
            t.cachedChestElytra = currentChest != null && currentChest.getType() == Material.ELYTRA;
            t.abilities.flying();
            t.packetsSinceMove = t.placementsSinceMove = t.digsSinceMove = t.actionsSinceMove = t.attacksSinceMove = t.slotsSinceMove = 0;
        } else if (packet.kind() == Observation.Kind.SWING) {
            boolean holdingFirework = player.getInventory().getItemInMainHand().getType().name().contains("FIREWORK")
                    || player.getInventory().getItemInOffHand().getType().name().contains("FIREWORK");
            var clickState = t.clicks.observeSwing(plugin.tick(), packet.values().getOrDefault("click-time-ms", (double) System.currentTimeMillis()).longValue(), player.getGameMode() == GameMode.CREATIVE, holdingFirework);
            values.put("click-delay-ms", (double) clickState.delayMs());
            plugin.data(player).clicksPerSecond = clickState.cps();
            if (clickState.cpsReady()) values.put("eligible-swing-cps", clickState.cps());
            states.add("click-context-known");
            if (clickState.nonDigExempt()) states.add("click-non-dig-exempt");
            if (clickState.statisticalExempt()) states.add("click-statistical-exempt");
            if (t.swingNanos > 0 && packet.nanos() >= t.swingNanos) values.put("swing-interval-ms", (packet.nanos() - t.swingNanos) / 1_000_000d);
            values.put("swing-interval-ticks", (double) Math.max(0, plugin.tick() - t.swingTick));
            t.swingNanos = packet.nanos(); t.swingTick = plugin.tick(); t.attacksSinceSwing = 0; t.swingsSinceAttack++;
        } else if (packet.kind() == Observation.Kind.ATTACK) {
            t.clicks.attack(); t.attackMovementAge = 0;
            int target = (int) values.getOrDefault("target-id", -1d).doubleValue();
            values.put("previous-target-id", (double) t.targetId);
            if (target != t.targetId) states.add("target-change");
            t.targetId = target; t.attackTick = plugin.tick(); t.attacksSinceMove++; t.attacksSinceSwing++; t.swingsSinceAttack = 0;
            attack(player, t, target, values, states);
            if (states.contains("target-player")) t.targetHistory.selectPlayer(target);
        } else if (packet.kind() == Observation.Kind.PLACE || packet.kind() == Observation.Kind.DIG || packet.kind() == Observation.Kind.BREAK) {
            long millis = packet.values().getOrDefault("packet-time-ms", (double) System.currentTimeMillis()).longValue();
            double blockX = packet.values().getOrDefault("block-x", 0d), blockZ = packet.values().getOrDefault("block-z", 0d);
            if (packet.state("dig:START_DIGGING")) t.clicks.digging(plugin.tick(), millis, ClickContext.DigAction.START, blockX, blockZ);
            if (packet.state("dig:FINISHED_DIGGING")) t.clicks.digging(plugin.tick(), millis, ClickContext.DigAction.FINISH, blockX, blockZ);
            if (packet.state("dig:CANCELLED_DIGGING")) t.clicks.digging(plugin.tick(), millis, ClickContext.DigAction.CANCEL, blockX, blockZ);
            if (packet.state("dig:DROP_ITEM") || packet.state("dig:DROP_ITEM_STACK")) t.clicks.digging(plugin.tick(), millis, ClickContext.DigAction.DROP, blockX, blockZ);
            if (packet.kind() == Observation.Kind.PLACE) t.placementsSinceMove++; else t.digsSinceMove++;
            if (packet.states().contains("dig:START_DIGGING")) { t.digging = true; t.digStarted = packet.nanos(); }
            if (packet.states().contains("dig:CANCELLED_DIGGING") || packet.states().contains("dig:FINISHED_DIGGING")) t.digging = false;
            if (!values.containsKey("dig-duration-ms") && t.digStarted > 0 && packet.nanos() >= t.digStarted) values.put("dig-duration-ms", (packet.nanos() - t.digStarted) / 1_000_000d);
            block(player, values, states);
        } else if (packet.kind() == Observation.Kind.ACKNOWLEDGEMENT) {
            if (packet.states().contains("ack-known")) {
                t.lastAckNanos = packet.nanos(); t.transactionPing = values.getOrDefault("ack-latency-ms", 0d);
                t.acknowledgementSeen = true;
                if (t.actualVelocitySeen && values.containsKey("ack-sent-nanos") && values.get("ack-sent-nanos") > t.actualVelocityNanos) {
                    t.velocityConfirmed = true;
                    t.velocityModel.acknowledge(values.get("ack-sent-nanos").longValue());
                }
            }
        } else if (packet.kind() == Observation.Kind.VELOCITY && packet.values().containsKey("velocity-y")) {
            t.velocity = new Vector(packet.values().getOrDefault("velocity-x", 0d),packet.values().get("velocity-y"),packet.values().getOrDefault("velocity-z", 0d));
            t.velocityTick = plugin.tick(); t.actualVelocityNanos = packet.nanos(); t.velocityConfirmed = false; t.actualVelocitySeen = true;
            t.velocityModel.outgoing(t.velocity.getX(), t.velocity.getY(), t.velocity.getZ(), packet.nanos());
        }
        if (packet.type().equals("HELD_ITEM_CHANGE")) {
            values.put("previous-slot", (double) t.slot); t.slot = (int) values.getOrDefault("slot", -1d).doubleValue(); t.slotsSinceMove++;
        }
        if (packet.type().equals("CLICK_WINDOW")) t.clicks.inventoryClick(plugin.tick());
        if (packet.state("client-status:PERFORM_RESPAWN")) t.events.mark(Tick.RESPAWN, plugin.tick());
        if (packet.states().contains("entity-interaction") || packet.kind() == Observation.Kind.ATTACK) {
            int targetId = packet.values().getOrDefault("target-id", -1d).intValue();
            if (player.getNearbyEntities(16, 16, 16).stream().anyMatch(entity -> entity.getEntityId() == targetId && entity instanceof ItemFrame)) t.clicks.itemFrameInteraction(plugin.tick());
        }
        if (packet.type().equals("ENTITY_ACTION")) {
            t.actionsSinceMove++;
            if (packet.state("action:START_SPRINTING")) t.sourceSprinting = true;
            if (packet.state("action:STOP_SPRINTING")) t.sourceSprinting = false;
            if (packet.state("action:START_SNEAKING")) t.sourceSneaking = true;
            if (packet.state("action:STOP_SNEAKING")) t.sourceSneaking = false;
            if (packet.states().contains("action:START_FLYING_WITH_ELYTRA")) {
                var chest = player.getInventory().getChestplate();
                if (!t.clientGround && !t.previousClientGround && values.getOrDefault("client-at-least-1-15", 0d) == 1
                        && chest != null && chest.getType() == Material.ELYTRA) t.abilities.elytraAction(true);
                if (t.elytraToggleNanos > 0 && packet.nanos() >= t.elytraToggleNanos) values.put("elytra-toggle-interval", (packet.nanos() - t.elytraToggleNanos) / 1_000_000d);
                states.add("elytra-start"); t.elytraToggleNanos = packet.nanos();
            }
        }
        if (packet.type().equals("STEER_VEHICLE") || packet.type().equals("PLAYER_INPUT")) {
            t.inputKnown = packet.values().containsKey("steer-sideways") && packet.values().containsKey("steer-forward");
            if (t.inputKnown) { t.strafeInput = packet.values().get("steer-sideways"); t.forwardInput = packet.values().get("steer-forward"); }
            if (t.inputKnown) {
                values.put("strafe-input", t.strafeInput); values.put("forward-input", t.forwardInput); states.add("movement-input-known");
            }
            t.inputJump = packet.states().contains("input-jump") || packet.states().contains("vehicle-jump");
            if (Math.abs(values.getOrDefault("steer-forward", 0d)) + Math.abs(values.getOrDefault("steer-sideways", 0d)) > 0) t.vehicleSteeredTick = plugin.tick();
        }
        stateValues(player, t, values, states);
        var collected = combatGeometry(player, t, t.protocolContext.update(new Observation(packet.kind(), plugin.tick(), packet.nanos(), values, states)));
        collected = t.combatExemptions.update(collected, t.sourceVelocity.snapshot());
        var observation = t.sequenceContext.update(t.velocityPrediction.update(collected, t.sourceVelocity.snapshot()));
        if (packet.kind() == Observation.Kind.MOVE && packet.state("rotation-changed")) sink.accept(player, new Observation(Observation.Kind.ROTATION, observation.tick(), observation.nanos(), observation.values(), observation.states()));
        sink.accept(player, observation);
        if (packet.kind() == Observation.Kind.MOVE && packet.state("position-packet")) t.sourceVelocity.afterPosition();
        if (packet.kind() != Observation.Kind.VELOCITY && packet.kind() != Observation.Kind.SERVER_POSITION) {
            t.lastPacketNanos = packet.nanos(); t.previousType = packet.type(); t.packetsSinceMove++;
        }
    }

    public void velocity(Player player, Vector velocity) {
        Tracking t = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        t.velocity = velocity.clone(); t.velocityTick = plugin.tick(); t.velocityConfirmed = false; t.actualVelocitySeen = false;
        t.velocityModel.invalidate();
    }

    public void teleport(Player player, Location destination, boolean grantsExemption) {
        Tracking t = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        t.position = destination.clone(); t.serverLocation = t.position.clone(); t.teleportTick = plugin.tick();
        t.dx = t.dy = t.dz = 0;
        t.lastMotion = Map.of(); t.velocityModel.invalidate(); t.actualVelocitySeen = t.velocityConfirmed = false;
        t.vehicleLocation = null; t.vehicleAirTicks = 0;
        if (grantsExemption) t.bukkitTeleportMoves = 0;
    }

    public void nonPearlTeleport(Player player) { sourceEvent(player, Age.NONPEARL_TELEPORT); }
    public void enderPearlTeleport(Player player) { sourceEvent(player, Age.ENDER_PEARL); }
    public void sourceDamage(Player player, String type) {
        Tracking state = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        state.events.damage(type);
    }

    public void sourceDamageCause(Player player, String cause) {
        Tracking state = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        state.events.damageCause(cause);
    }

    public void event(Player player, Observation.Kind kind, Map<String, Double> values, Set<String> states) {
        Tracking t = tracking.computeIfAbsent(player.getUniqueId(), ignored -> new Tracking(player.getLocation()));
        if (!t.transportComplete) return;
        if (states.contains("block-accepted")) {
            if (kind == Observation.Kind.PLACE) t.placeTick = plugin.tick();
            if (kind == Observation.Kind.BREAK) t.breakTick = plugin.tick();
        }
        Map<String, Double> measurements = new HashMap<>(t.context);
        measurements.putAll(t.lastMotion); measurements.putAll(values);
        measurements.put("event-time-ms", (double) System.currentTimeMillis());
        Set<String> flags = new HashSet<>(t.states); flags.addAll(states); flags.add("bukkit-event");
        stateValues(player, t, measurements, flags);
        blockGrace(t, flags);
        connectionValues(t, measurements, flags);
        measurements.put("ghost-block-setbacks", (double) t.ghostSetbacks);
        if (states.contains("ghosthand-callback")) ghostHand(player, t, measurements, flags);
        else if (!states.contains("fastbreak-callback") && (kind == Observation.Kind.PLACE || kind == Observation.Kind.BREAK || states.contains("block-interaction"))) block(player, measurements, flags);
        if (kind == Observation.Kind.PLACE) placement(player, t, measurements, flags);
        if (states.contains("fastbreak-callback")) {
            if (t.acknowledgedState.containsKey("protocol-conduit-power")) flags.add("fastbreak-context-known");
            measurements.put("mining-deadline-difference-ms", t.mining.difference(measurements.get("event-time-ms").longValue()));
            if (player.getGameMode() == GameMode.SURVIVAL) flags.add("source-survival");
            if (values.keySet().containsAll(Set.of("block-x", "block-y", "block-z"))) {
                var material = player.getWorld().getBlockAt(values.get("block-x").intValue(), values.get("block-y").intValue(), values.get("block-z").intValue()).getType().name();
                if (io.github.lino9999.linoac.detection.world.MiningContext.materialExempt(material)) flags.add("fastbreak-material-exempt");
            }
            if (io.github.lino9999.linoac.detection.world.MiningContext.toolExempt(player.getInventory().getItemInMainHand().getType().name(), player.getInventory().getItemInOffHand().getType().name())) flags.add("fastbreak-tool-exempt");
            var helmet = player.getInventory().getHelmet();
            if (player.hasPotionEffect(PotionEffectType.HASTE) || t.acknowledgedState.getOrDefault("protocol-conduit-power", 0d) > 0
                    || helmet != null && helmet.containsEnchantment(Enchantment.AQUA_AFFINITY)
                    || player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.EFFICIENCY) > 5) flags.add("fastbreak-ability");
        }
        sink.accept(player, t.sequenceContext.update(new Observation(kind, plugin.tick(), System.nanoTime(), measurements, flags)));
    }

    private void attack(Player player, Tracking t, int entityId, Map<String, Double> values, Set<String> states) {
        states.remove("target-player"); states.remove("living-target");
        for (String key : List.of("reach-distance","reach-limit","hitbox-distance","reach-current-distance","target-distance","target-width","target-horizontal","aim-angle")) values.remove(key);
        Entity target = player.getWorld().getPlayers().stream().filter(entity -> entity.getEntityId() == entityId).findFirst().map(entity -> (Entity) entity)
                .orElseGet(() -> player.getNearbyEntities(16, 16, 16).stream().filter(entity -> entity.getEntityId() == entityId).findFirst().orElse(null));
        if (target == null) return;
        Vector eye = t.position.toVector().add(new Vector(0, player.getEyeHeight(), 0));
        Vector look = t.position.getDirection();
        List<BoundingBox> boxes = new ArrayList<>(); boxes.add(target.getBoundingBox().clone());
        if (target instanceof Player other) boxes.addAll(plugin.data(other).history);
        double distance = boxes.stream().mapToDouble(box -> Geometry.distance(eye, box)).min().orElse(0);
        double range = Environment.attribute(player, "entity_interaction_range", 3) + plugin.settings().number("limits.reach-margin") + Math.min(.6, player.getPing() * .002);
        double hitbox = boxes.stream().map(box -> box.clone().expand(.15).rayTrace(eye, look, range + 1))
                .filter(Objects::nonNull).mapToDouble(hit -> eye.distance(hit.getHitPosition())).min().orElse(-1);
        Vector direction = target.getBoundingBox().getCenter().subtract(eye);
        values.put("reach-distance", distance); values.put("reach-limit", range); values.put("hitbox-distance", hitbox);
        values.put("reach-current-distance", Geometry.distance(eye, target.getBoundingBox()));
        values.put("target-distance", eye.distance(target.getBoundingBox().getCenter()));
        values.put("target-width", target.getBoundingBox().getWidthX());
        if (target instanceof Player other) {
            states.add("target-player");
            values.put("source-target-max-no-damage-ticks", (double) other.getMaximumNoDamageTicks());
            values.put("target-max-no-damage-ticks", (double) other.getMaximumNoDamageTicks());
            Tracking targetTracking = tracking.get(other.getUniqueId());
            if (targetTracking != null && targetTracking.lastMotion.containsKey("horizontal")) values.put("target-horizontal", targetTracking.lastMotion.get("horizontal"));
        }
        values.put("aim-angle", direction.lengthSquared() == 0 ? 0d : Math.toDegrees(direction.angle(look)));
        values.put("attack-horizontal-distance", Math.hypot(player.getLocation().getX() - target.getLocation().getX(), player.getLocation().getZ() - target.getLocation().getZ()) - .42);
        if (target instanceof LivingEntity) states.add("living-target");
    }

    private void targetMotion(Player player, Tracking tracking, Map<String, Double> values, Set<String> states) {
        values.remove("target-horizontal"); states.remove("target-player");
        for (Player target : player.getWorld().getPlayers()) if (target.getEntityId() == tracking.targetId) {
            states.add("target-player");
            values.put("target-max-no-damage-ticks", (double) target.getMaximumNoDamageTicks());
            Tracking targetState = this.tracking.get(target.getUniqueId());
            if (targetState != null && targetState.lastMotion.containsKey("horizontal")) values.put("target-horizontal", targetState.lastMotion.get("horizontal"));
            return;
        }
    }

    private Observation combatGeometry(Player player, Tracking state, Observation input) {
        if (input.kind() != Observation.Kind.MOVE || !input.hasAll("packet-time-ms", "observation-server-tick")) return input;
        var values = new HashMap<>(input.values());
        var flags = new HashSet<>(input.states());
        var rawPosition = state.positionState.snapshot();
        double x = rawPosition.rawX(), y = rawPosition.rawY(), z = rawPosition.rawZ();
        long tick = (long) input.value("observation-server-tick");
        int lag = (int) Math.min(Integer.MAX_VALUE, Math.floor(state.transactionPing / 50) + 3);
        state.targetHistory.flying(x, z, tick, lag);
        double historicalDistance = state.targetHistory.distance(x, z, tick, lag);
        double rawYaw = Math.toRadians(state.cinematic.rawYaw()), rawPitch = Math.toRadians(state.cinematic.rawPitch());
        double lookHorizontal = Math.cos(rawPitch);
        double historicalAngle = state.targetHistory.angle(x, z, -lookHorizontal * Math.sin(rawYaw), lookHorizontal * Math.cos(rawYaw), tick, lag);
        if (historicalDistance == -1) { flags.add("source-reach-a-known"); values.put("source-reach-a-distance", -1d); }
        if (historicalAngle == -1) { flags.add("source-hitbox-a-known"); values.put("source-hitbox-a-angle", -1d); }
        Player historicalTarget = player.getWorld().getPlayers().stream().filter(target -> target.getEntityId() == state.targetHistory.target()).findFirst().orElse(null);
        Tracking historicalData = historicalTarget == null ? null : tracking.get(historicalTarget.getUniqueId());
        boolean recentFast = input.value("packet-time-ms") - input.value("protocol-last-fast-move-ms") < 1500;
        boolean vehicle = input.value("timer-vehicle-exempt") == 1;
        if (historicalTarget != null && historicalData != null && historicalData.transportComplete && historicalData.positionState.snapshot().positionKnown()
                && input.has("protocol-cached-creative")) {
            boolean targetVehicle = historicalData.positionState.snapshot().vehiclePositionAge() < 25 || historicalTarget.isInsideVehicle() || historicalTarget.getVehicle() != null;
            Double targetBoat = historicalData.cachedWorld.snapshot().boatContact(historicalData.positionGeometry.get("source-nearby-boat-contact"));
            boolean targetExempt = targetVehicle || targetBoat != null && targetBoat == 1;
            var reachExempt = ProtocolExemptions.evaluate(input, "CREATIVE", "VEHICLE", "BOAT");
            var hitboxExempt = ProtocolExemptions.evaluate(input, "CREATIVE", "GLIDING", "VEHICLE");
            if (input.hasAll("protocol-attack-range", "protocol-exempt-LIQUID") && (reachExempt.active() || targetExempt || reachExempt.known() && targetBoat != null)) flags.add("source-reach-a-known");
            if (hitboxExempt.active() || targetExempt || hitboxExempt.known() && targetBoat != null) flags.add("source-hitbox-a-known");
            if (historicalDistance != -1 && plugin.data(historicalTarget).bedrock && plugin.settings().enabled("settings.ignore-bedrock")) flags.add("source-reach-a-early-return");
            if (reachExempt.active() || targetExempt || player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK) > 0) flags.add("source-reach-a-exempt");
            if (hitboxExempt.active() || targetExempt) flags.add("source-hitbox-a-exempt");
            values.put("source-reach-a-distance", historicalDistance);
            values.put("source-reach-a-limit", input.value("protocol-attack-range") + plugin.settings().number("checks.reach-a.max-reach") - 3
                    + (recentFast ? 1.15 : 0) + (input.value("protocol-exempt-LIQUID") == 1 ? .1 : 0));
            values.put("source-hitbox-a-angle", historicalAngle);
            values.put("source-hitbox-a-limit", plugin.settings().number("checks.hitbox-a.max-angle") + (recentFast ? .25 : 0));
            values.put("source-target-horizontal-distance", state.targetHistory.cachedDistance());
        }
        Player currentTarget = player.getWorld().getPlayers().stream().filter(target -> target.getEntityId() == state.targetId).findFirst().orElse(null);
        Tracking currentData = currentTarget == null ? null : tracking.get(currentTarget.getUniqueId());
        if (currentData != null && currentData.transportComplete && currentData.positionState.snapshot().positionKnown() && input.hasAll("protocol-attack-range", "protocol-speed-amplifier", "source-horizontal", "protocol-ground-run", "protocol-cached-creative")) {
            var targetPosition = currentData.positionState.snapshot();
            double dx = x - targetPosition.rawX(), dz = z - targetPosition.rawZ();
            double limit = input.value("protocol-attack-range") + plugin.settings().number("checks.reach-a.max-reach") - 3 + .05
                    + Math.abs(y - targetPosition.rawY()) * .9 + input.value("source-horizontal") * .66;
            if (state.sourceSprinting) limit += .32;
            if (input.value("protocol-ground-run") < 3) limit += .35;
            if (input.value("protocol-speed-amplifier") > 0) limit += input.value("protocol-speed-amplifier") * .125;
            if (Math.abs(180 - Math.abs(player.getLocation().getYaw() - currentTarget.getLocation().getYaw())) > 100) limit += 1.25;
            if (input.value("teleport-movement-age") < 8 || recentFast) limit += 1;
            boolean targetVehicle = targetPosition.vehiclePositionAge() < 25 || currentTarget.isInsideVehicle() || currentTarget.getVehicle() != null;
            Double targetBoat = currentData.cachedWorld.snapshot().boatContact(currentData.positionGeometry.get("source-nearby-boat-contact"));
            boolean targetExempt = targetVehicle || targetBoat != null && targetBoat == 1;
            var reachExempt = ProtocolExemptions.evaluate(input, "TELEPORT", "JOINED", "PLUGIN_LOAD", "WORLD_CHANGE", "CREATIVE", "VEHICLE");
            if (reachExempt.active() || targetExempt || reachExempt.known() && targetBoat != null) flags.add("source-reach-b-known");
            if (reachExempt.active() || targetExempt) flags.add("source-reach-b-exempt");
            values.put("source-reach-b-distance", Math.sqrt(dx * dx + dz * dz) - .565);
            values.put("source-reach-b-limit", limit);
        }
        return new Observation(input.kind(), input.tick(), input.nanos(), values, flags);
    }

    private void placement(Player player, Tracking tracking, Map<String, Double> values, Set<String> states) {
        values.put("world-token", (double) worldTokens.computeIfAbsent(player.getWorld().getUID(), ignored -> ++nextWorldToken));
        if (player.getAllowFlight() || player.isFlying()) states.add("source-flight"); else states.remove("source-flight");
        if (player.getGameMode() == GameMode.CREATIVE) states.add("source-creative"); else states.remove("source-creative");
        if (player.isInsideVehicle()) states.add("vehicle"); else states.remove("vehicle");
        if (player.getInventory().getItemInMainHand().getType().name().contains("HOE") || player.getInventory().getItemInOffHand().getType().name().contains("HOE")) states.add("held-hoe");
        if (values.keySet().containsAll(Set.of("block-x", "block-y", "block-z"))) {
            int x = values.get("block-x").intValue(), y = values.get("block-y").intValue(), z = values.get("block-z").intValue();
            if (loaded(player, x, y, z)) {
                String material = player.getWorld().getBlockAt(x, y, z).getType().name();
                states.add("bridge-material-known"); states.add("scaffold-n-material-known");
                states.add("fastplace-material-known");
                if (io.github.lino9999.linoac.detection.world.PlacementContext.fastPlaceMaterialExempt(material)) states.add("fastplace-material-exempt");
                if (io.github.lino9999.linoac.detection.world.PlacementContext.bridgeMaterialExempt(material)) states.add("bridge-material-exempt");
                if (io.github.lino9999.linoac.detection.world.PlacementContext.scaffoldNMaterialExempt(material)) states.add("scaffold-n-material-exempt");
            }
        }
        Location server = tracking.serverDestination;
        if (server != null && server.getWorld().equals(player.getWorld()) && tracking.serverWorld.complete()) {
            states.add("block-two-below-known");
            if (tracking.serverWorld.twoBelowAir()) states.add("block-two-below-air");
            values.put("bridge-air-samples", (double) tracking.serverWorld.airBelow());
        }
        int solids = 0; boolean known = true;
        Vector direction = tracking.position.getDirection();
        for (int distance = 0; distance <= 3; distance++) {
            Location point = player.getLocation().add(direction.clone().multiply(distance));
            if (!loaded(player, point.getBlockX(), point.getBlockY(), point.getBlockZ())) known = false;
            else if (player.getWorld().getBlockAt(point.getBlockX(), point.getBlockY(), point.getBlockZ()).getType().isSolid()) solids++;
        }
        if (known) values.put("feet-solid-samples", (double) solids);
    }

    private void ghostHand(Player player, Tracking tracking, Map<String, Double> values, Set<String> states) {
        var position = tracking.positionState.snapshot();
        if (!position.positionKnown() || !values.keySet().containsAll(Set.of("block-x", "block-y", "block-z"))) return;
        int x = values.get("block-x").intValue(), y = values.get("block-y").intValue(), z = values.get("block-z").intValue();
        if (!loaded(player, x, y, z)) return;
        if (!io.github.lino9999.linoac.detection.world.GhostHandContext.bed(player.getWorld().getBlockAt(x, y, z).getType().name())) return;
        String[] adjacent = new String[4];
        int[][] offsets = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
        for (int index = 0; index < offsets.length; index++) {
            int sampleX = x + offsets[index][0], sampleZ = z + offsets[index][1];
            if (!loaded(player, sampleX, y, sampleZ)) return;
            adjacent[index] = player.getWorld().getBlockAt(sampleX, y, sampleZ).getType().name();
        }
        var direction = io.github.lino9999.linoac.detection.world.GhostHandContext.direction(adjacent[0], adjacent[1], adjacent[2], adjacent[3]);
        if (direction == null) return;
        var materials = new ArrayList<io.github.lino9999.linoac.detection.world.GhostHandContext.MaterialSample>(10);
        for (var offset : direction.offsets()) {
            if (!loaded(player, x + offset.x(), y + offset.y(), z + offset.z())) return;
            Material material = player.getWorld().getBlockAt(x + offset.x(), y + offset.y(), z + offset.z()).getType();
            materials.add(new io.github.lino9999.linoac.detection.world.GhostHandContext.MaterialSample(material.name(), material.isSolid(), material.isOccluding()));
        }
        var enclosure = io.github.lino9999.linoac.detection.world.GhostHandContext.enclosure(materials);
        values.put("ghosthand-wire-y", position.rawY());
        values.put("ghosthand-opaque-blocks", (double) enclosure.opaqueBlocks());
        values.put("ghosthand-glass-blocks", (double) enclosure.glassBlocks());
        states.add("ghosthand-context-known");
    }

    private void block(Player player, Map<String, Double> values, Set<String> states) {
        if (!values.keySet().containsAll(List.of("block-x","block-y","block-z"))) return;
        int x = values.get("block-x").intValue(), y = values.get("block-y").intValue(), z = values.get("block-z").intValue();
        if (y < player.getWorld().getMinHeight() || y >= player.getWorld().getMaxHeight() || !player.getWorld().isChunkLoaded(x >> 4, z >> 4)) { states.add("block-unloaded"); return; }
        Block block = player.getWorld().getBlockAt(x, y, z);
        Location center = block.getLocation().add(.5, .5, .5);
        values.put("block-distance", player.getEyeLocation().distance(center));
        double breakSpeed = block.getBreakSpeed(player);
        values.put("break-speed", breakSpeed);
        if (Double.isFinite(breakSpeed) && breakSpeed > 0) values.put("expected-break-ms", Math.ceil(1 / breakSpeed) * 50);
        values.put("block-relative-y", center.getY() - player.getEyeLocation().getY());
        if (block.isPassable()) states.add("block-passable");
        if (block.getType().name().contains("BED")) states.add("bed");
        Boolean obstructed = obstructed(player, center);
        if (Boolean.TRUE.equals(obstructed)) states.add("block-obstructed");
        if (obstructed == null) states.add("block-path-unknown");
        if (obstructed != null) states.add("block-ray-known");
        if (Boolean.TRUE.equals(obstructed)) states.add("block-ray-obstructed");
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed bed) {
            var direction = bed.getFacing();
            int sign = bed.getPart() == org.bukkit.block.data.type.Bed.Part.FOOT ? 1 : -1;
            int otherX = x + direction.getModX() * sign, otherZ = z + direction.getModZ() * sign;
            if (!loaded(player, otherX, y, otherZ)) states.remove("block-ray-known");
            else {
                Boolean paired = obstructed(player, new Location(player.getWorld(), otherX + .5, y + .5, otherZ + .5));
                if (paired == null) states.remove("block-ray-known");
                else if (!paired) states.add("paired-bed-visible");
            }
        }
        boolean support = false;
        boolean supportKnown = true;
        for (var face : new org.bukkit.block.BlockFace[]{org.bukkit.block.BlockFace.UP,org.bukkit.block.BlockFace.DOWN,org.bukkit.block.BlockFace.NORTH,org.bukkit.block.BlockFace.SOUTH,org.bukkit.block.BlockFace.EAST,org.bukkit.block.BlockFace.WEST}) {
            int adjacentX = x + face.getModX(), adjacentY = y + face.getModY(), adjacentZ = z + face.getModZ();
            if (!loaded(player, adjacentX, adjacentY, adjacentZ)) { states.add("block-unloaded"); supportKnown = false; continue; }
            Block adjacent = player.getWorld().getBlockAt(adjacentX, adjacentY, adjacentZ);
            if (!adjacent.getType().isAir() && !adjacent.isLiquid()) support = true;
        }
        if (support) states.add("block-supported");
        if (supportKnown) states.add("block-support-known");
    }

    private void environment(Player player, Map<String, Double> values, Set<String> states) {
        Location l = player.getLocation();
        BoundingBox body = player.getBoundingBox();
        if (body.getWidthX() > 2 || body.getHeight() > 3 || body.getHeight() < .3) {
            states.add("environment-unknown"); states.add("environment-special");
            return;
        }
        BoundingBox feet = new BoundingBox(body.getMinX() + .01, body.getMinY() - .15, body.getMinZ() + .01,
                body.getMaxX() - .01, body.getMinY() + .01, body.getMaxZ() - .01);
        BoundingBox head = new BoundingBox(body.getMinX() + .01, body.getMaxY() - .01, body.getMinZ() + .01,
                body.getMaxX() - .01, body.getMaxY() + .1, body.getMaxZ() - .01);
        BoundingBox nearFeet = feet.clone();
        nearFeet.resize(nearFeet.getMinX(), nearFeet.getMinY() - .5, nearFeet.getMinZ(), nearFeet.getMaxX(), nearFeet.getMaxY(), nearFeet.getMaxZ());
        BoundingBox sides = body.clone().expand(.06, 0, .06);
        sides.resize(sides.getMinX(), body.getMinY() + .1, sides.getMinZ(), sides.getMaxX(), body.getMaxY() - .1, sides.getMaxZ());
        boolean collision = false, known = true;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (!player.getWorld().isChunkLoaded((l.getBlockX() + dx) >> 4, (l.getBlockZ() + dz) >> 4)) { known = false; continue; }
            for (int dy = -2; dy <= 3; dy++) {
                if (!loaded(player, l.getBlockX() + dx, l.getBlockY() + dy, l.getBlockZ() + dz)) continue;
                Block block = player.getWorld().getBlockAt(l.getBlockX() + dx, l.getBlockY() + dy, l.getBlockZ() + dz);
                String name = block.getType().name();
                BoundingBox volume = new BoundingBox(block.getX(), block.getY(), block.getZ(), block.getX() + 1, block.getY() + 1, block.getZ() + 1);
                boolean touchesBody = volume.overlaps(body), underFeet = volume.overlaps(feet);
                if (name.contains("WEB") && touchesBody) states.add("web");
                if (name.equals("SOUL_SAND") && underFeet) states.add("soul-sand");
                if (name.equals("POWDER_SNOW") && touchesBody) states.add("powder-snow");
                if (name.contains("ICE") && underFeet) states.add("ice");
                if (name.contains("SLIME") && underFeet) states.add("slime");
                if (block.isLiquid() && underFeet) states.add("liquid-below");
                if (name.equals("BUBBLE_COLUMN") && touchesBody) states.add("bubble-column");
                if (name.contains("PISTON") && (touchesBody || underFeet || volume.overlaps(sides))) states.add("piston");
                if ((touchesBody || underFeet) && (block.isLiquid() || name.contains("ICE") || name.contains("SLIME") || name.contains("HONEY")
                        || name.contains("WEB") || name.contains("BERRY") || name.contains("POWDER_SNOW") || name.contains("SOUL_SAND")
                        || name.contains("BUBBLE") || name.contains("BED") || name.contains("PISTON"))) states.add("environment-special");
                if (!block.isPassable() && !block.isLiquid()) {
                    if (block.getCollisionShape().overlaps(nearFeet.clone().shift(-block.getX(), -block.getY(), -block.getZ()))) states.add("near-ground");
                    if (block.getCollisionShape().overlaps(head.clone().shift(-block.getX(), -block.getY(), -block.getZ()))) states.add("head-blocked");
                    if (block.getCollisionShape().overlaps(sides.clone().shift(-block.getX(), -block.getY(), -block.getZ()))) collision = true;
                }
            }
        }
        if (known) states.add("support-known"); else { states.add("environment-unknown"); states.add("environment-special"); }
        if (collision) states.add("horizontal-collision");
        if (known && !collision && !states.contains("head-blocked") && !states.contains("environment-special")) states.add("unobstructed");
        values.put("friction", states.contains("ice") ? .98 : states.contains("slime") ? .8 : .6);
    }

    private void vehicle(Player player, Tracking t, Map<String, Double> values, Set<String> states) {
        Entity vehicle = player.getVehicle();
        if (vehicle == null) { t.vehicleLocation = null; t.vehicleId = null; t.vehicleAirTicks = 0; t.vehicleDy = 0; return; }
        states.add("vehicle");
        Location current = vehicle.getLocation();
        boolean changed = !vehicle.getUniqueId().equals(t.vehicleId);
        Location previous = t.vehicleLocation == null || changed || !current.getWorld().equals(t.vehicleLocation.getWorld()) ? current : t.vehicleLocation;
        values.put("vehicle-dy", current.getY() - previous.getY());
        values.put("previous-vehicle-dy", t.vehicleDy);
        values.put("vehicle-horizontal", Math.hypot(current.getX() - previous.getX(), current.getZ() - previous.getZ()));
        if (vehicle.isOnGround()) { states.add("vehicle-ground"); t.vehicleAirTicks = 0; } else t.vehicleAirTicks++;
        values.put("vehicle-air-ticks", (double) t.vehicleAirTicks);
        if (vehicle.isInWater()) states.add("vehicle-liquid");
        if (plugin.tick() - t.vehicleSteeredTick < 4) states.add("vehicle-steered");
        if (vehicle.getType().name().contains("BOAT") || vehicle.getType().name().contains("RAFT")) states.add("boat");
        if (vehicle instanceof AbstractHorse horse) {
            states.add("horse");
            if (horse.getInventory().getSaddle() != null) states.add("saddled");
            values.put("vehicle-jump-strength", horse.getJumpStrength());
        }
        if (vehicle instanceof LivingEntity living) {
            var key = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("movement_speed"));
            var attribute = key == null ? null : living.getAttribute(key);
            if (attribute != null) values.put("vehicle-speed", attribute.getValue());
            if (living.hasPotionEffect(PotionEffectType.LEVITATION) || living.hasPotionEffect(PotionEffectType.SLOW_FALLING)) states.add("vehicle-special");
        }
        if (vehicle instanceof Minecart minecart) values.put("vehicle-speed", minecart.getMaxSpeed());
        if (vehicle instanceof Steerable steerable && steerable.hasSaddle()) states.add("saddled");
        if (vehicle instanceof Camel camel && camel.isDashing()) t.vehicleSpecialUntil = plugin.tick() + 40;
        if (changed) t.vehicleSpecialUntil = Math.max(t.vehicleSpecialUntil, plugin.tick() + 20);
        if (plugin.tick() < t.vehicleSpecialUntil || states.contains("ice") || states.contains("slime")) states.add("vehicle-special");
        t.vehicleId = vehicle.getUniqueId(); t.vehicleDy = values.get("vehicle-dy"); t.vehicleLocation = current;
    }

    private void connectionValues(Tracking t, Map<String, Double> values, Set<String> states) {
        states.remove("transaction-tracked");
        values.remove("pending-transactions"); values.remove("transaction-age-ms"); values.remove("transaction-ping");
        if (t.connectionTracked) {
            states.add("transaction-tracked");
            values.put("pending-transactions", (double) t.pendingTransactions);
            values.put("transaction-age-ms", t.oldestTransactionMs);
        }
        if (t.acknowledgementSeen) values.put("transaction-ping", t.transactionPing);
    }

    private void blockGrace(Tracking tracking, Set<String> states) {
        states.remove("block-place-grace"); states.remove("block-break-grace"); states.remove("break-grace");
        if (plugin.tick() - tracking.placeTick < 3) states.add("block-place-grace");
        if (plugin.tick() - tracking.breakTick < 3) { states.add("block-break-grace"); states.add("break-grace"); }
    }

    private void stateValues(Player player, Tracking tracking, Map<String, Double> values, Set<String> states) {
        var position = tracking.positionState.snapshot();
        var camera = tracking.cinematic.snapshot();
        var world = tracking.cachedWorld.snapshot();
        var abilities = tracking.abilities.snapshot();
        var velocity = tracking.sourceVelocity.snapshot();
        for (String key : List.of("source-explosion-pending", "source-explosion-sum-y", "source-last-positive-explosion-y", "source-explosion-position-age", "source-velocity-position-age",
                "source-maximum-horizontal-velocity", "source-maximum-vertical-velocity", "source-velocity-x", "source-velocity-z", "source-velocity-y", "source-velocity-vertical-age",
                "source-velocity-pending", "source-pending-velocity-count", "source-pending-explosion-count")) values.remove(key);
        if (velocity.complete()) {
            values.put("source-explosion-pending", velocity.explosionPending() ? 1d : 0d);
            values.put("source-explosion-sum-y", velocity.explosionSumY()); values.put("source-last-positive-explosion-y", velocity.lastPositiveExplosionY());
            values.put("source-explosion-position-age", (double) velocity.explosionPositionAge());
            values.put("source-velocity-position-age", (double) velocity.velocityPositionAge());
            values.put("source-maximum-horizontal-velocity", velocity.maximumHorizontal()); values.put("source-maximum-vertical-velocity", velocity.maximumVertical());
            values.put("source-velocity-x", velocity.velocityX()); values.put("source-velocity-z", velocity.velocityZ()); values.put("source-velocity-y", velocity.acknowledgedVelocityY());
            values.put("source-velocity-vertical-age", (double) velocity.verticalPositionAge());
            values.put("source-velocity-pending", velocity.velocityPending() ? 1d : 0d); values.put("source-pending-velocity-count", (double) velocity.pendingVelocities());
            values.put("source-pending-explosion-count", (double) velocity.pendingExplosions());
        }
        for (String key : ACK_STATE_KEYS) values.remove(key);
        values.putAll(tracking.acknowledgedState);
        values.put("eligible-swing-cps", plugin.data(player).clicksPerSecond);
        long packetMillis = values.getOrDefault("packet-time-ms", (double) System.currentTimeMillis()).longValue();
        long joinedAge = Math.max(0, packetMillis - plugin.data(player).joinedMillis);
        values.put("join-age-ms", (double) joinedAge); values.put("total-movement-packets", (double) tracking.totalMoves);
        if (joinedAge < plugin.settings().integer("settings.join-check-wait-time") || tracking.totalMoves < plugin.settings().integer("settings.min-ticks-existed")) states.add("joined");
        else states.remove("joined");
        values.put("protocol-exempt-JOINED", states.contains("joined") ? 1d : 0d);
        values.put("protocol-exempt-PLUGIN_LOAD", packetMillis - plugin.startedMillis() < 10000 ? 1d : 0d);
        states.remove("entity-cram"); states.remove("cinematic");
        if (tracking.positionState.cramExempt(plugin.settings().enabled("integrations.entity-cram.enabled"),
                plugin.settings().integer("integrations.entity-cram.grace-ticks"))) states.add("entity-cram");
        values.put("protocol-exempt-ENTITY_CRAM_FIX", states.contains("entity-cram") ? 1d : 0d);
        if (plugin.settings().enabled("settings.cinematic") && camera.cinematic()) states.add("cinematic");
        values.put("entity-cram-movement-age", (double) position.cramPositionAge());
        values.put("entity-cram-position-age", (double) position.cramPositionAge());
        if (tracking.serverNearbyEntities >= 0) values.put("server-nearby-entities", (double) tracking.serverNearbyEntities);
        values.put("cinematic-estimate", camera.cinematic() ? 1d : 0d);
        values.put("pitch-quantum", (double) camera.pitchQuantum());
        values.put("instant-sensitivity", camera.sensitivity());
        values.put("attack-movement-age", (double) tracking.attackMovementAge);
        tracking.events.values(values, plugin.tick());
        values.put("protocol-exempt-COMBO_MODE", tracking.events.age(Age.MELEE_DAMAGE) < 25 && player.getMaximumNoDamageTicks() < 15 ? 1d : 0d);
        values.put("protocol-exempt-MYTHIC_MOB", 0d);
        values.put("protocol-exempt-SLEEPING", player.isSleeping() || tracking.events.elapsed(Tick.BED_ENTER, plugin.tick()) < 40 || tracking.events.elapsed(Tick.BED_LEAVE, plugin.tick()) < 40 ? 1d : 0d);
        values.put("protocol-exempt-VOID", position.rawY() < -70 ? 1d : 0d);
        boolean netherite = Arrays.stream(player.getInventory().getArmorContents()).filter(Objects::nonNull).anyMatch(item -> item.getType().name().startsWith("NETHERITE_"));
        values.put("protocol-exempt-NETHERITE_ARMOR", netherite ? 1d : 0d);
        if (netherite) states.add("source-netherite-armor"); else states.remove("source-netherite-armor");
        values.put("source-server-position-match", tracking.serverTeleportMatch ? 1d : 0d);
        values.put("protocol-ghost-corrections", (double) tracking.ghostCorrections);
        values.put("protocol-server-at-least-1-9", 1d);
        values.put("source-server-at-least-1-9", 1d);
        values.put("protocol-server-at-least-1-21-2", 1d);
        values.put("protocol-bukkit-flight-allowed", player.getAllowFlight() ? 1d : 0d);
        values.put("protocol-bukkit-creative", player.getGameMode() == GameMode.CREATIVE ? 1d : 0d);
        values.put("protocol-bukkit-spectator", player.getGameMode() == GameMode.SPECTATOR ? 1d : 0d);
        values.put("protocol-bukkit-dead", player.isDead() ? 1d : 0d);
        values.put("protocol-exempt-FROZEN", plugin.data(player).frozen ? 1d : 0d);
        for (String key : List.of("protocol-exempt-SLOW_FALLING", "protocol-exempt-LEVITATION", "protocol-exempt-RIPTIDE", "protocol-exempt-GLIDING", "protocol-exempt-SWIMMING")) values.remove(key);
        if (abilities.slowKnown()) values.put("protocol-exempt-SLOW_FALLING", abilities.slowAge() < 40 ? 1d : 0d);
        if (abilities.levitationKnown()) values.put("protocol-exempt-LEVITATION", abilities.levitationAge() < 75 ? 1d : 0d);
        if (abilities.riptideKnown()) values.put("protocol-exempt-RIPTIDE", abilities.riptideAge() < 80 ? 1d : 0d);
        if (abilities.glidingKnown()) values.put("protocol-exempt-GLIDING", abilities.glidingAge() < 10 ? 1d : 0d);
        if (abilities.swimmingKnown()) values.put("protocol-exempt-SWIMMING", abilities.swimmingAge() < 40 ? 1d : 0d);
        boolean elytra = tracking.cachedChestElytra || abilities.elytraAge() < 100;
        values.put("protocol-exempt-ELYTRA", elytra ? 1d : 0d);
        values.put("protocol-exempt-DOLPHINS_GRACE", abilities.dolphinAge() < 50 || player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE) ? 1d : 0d);
        values.put("protocol-exempt-JUMP_BOOST_RAN_OUT", 0d);
        String mainHand = player.getInventory().getItemInMainHand().getType().name();
        String offHand = player.getInventory().getItemInOffHand().getType().name();
        values.put("protocol-held-bucket", mainHand.contains("BUCKET") || offHand.contains("BUCKET") ? 1d : 0d);
        values.put("protocol-invalid-j-threshold", plugin.settings().number("checks.speed-b.min-difference"));
        values.put("protocol-invalid-j-entity-fix", plugin.settings().enabled("settings.entity-collision") ? 1d : 0d);
        values.put("source-max-no-damage-ticks", (double) player.getMaximumNoDamageTicks());
        values.put("max-no-damage-ticks", (double) player.getMaximumNoDamageTicks());
        values.put("source-walk-speed", (double) .2f);
        if (tracking.acknowledgedState.containsKey("protocol-speed-amplifier")) values.put("source-speed-amplifier", tracking.acknowledgedState.get("protocol-speed-amplifier"));
        else values.remove("source-speed-amplifier");
        values.put("source-sprinting", tracking.sourceSprinting ? 1d : 0d);
        values.put("source-sneaking", tracking.sourceSneaking ? 1d : 0d);
        values.put("source-yaw", (double) tracking.cinematic.rawYaw()); values.put("source-pitch", (double) tracking.cinematic.rawPitch());
        values.put("source-previous-ground", tracking.previousClientGround ? 1d : 0d);
        values.put("source-previous-previous-ground", tracking.sourcePreviousPreviousGround ? 1d : 0d);
        if (tracking.acknowledgedState.containsKey("protocol-jump-strength")) values.put("source-jump-motion", tracking.acknowledgedState.get("protocol-jump-strength"));
        else values.remove("source-jump-motion");
        if (mainHand.contains("SWORD")) states.add("source-held-sword"); else states.remove("source-held-sword");
        if (mainHand.contains("SWORD")) states.add("blocking-weapon"); else states.remove("blocking-weapon");
        if (player.getGameMode() == GameMode.SPECTATOR) states.add("spectator"); else states.remove("spectator");
        if (mainHand.equals("BOW") || offHand.equals("BOW")) states.add("bow-held"); else states.remove("bow-held");
        if (mainHand.contains("CROSS") || offHand.contains("CROSS")) states.add("crossbow-held"); else states.remove("crossbow-held");
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.MULTISHOT)
                || player.getInventory().getItemInOffHand().containsEnchantment(Enchantment.MULTISHOT)) states.add("multishot-held"); else states.remove("multishot-held");
        values.put("source-fastbow-exempt-known", elytra || abilities.glidingKnown() ? 1d : 0d);
        values.put("source-fastbow-exempt", elytra || abilities.glidingKnown() && abilities.glidingAge() < 10 ? 1d : 0d);
        for (String key : WORLD_POSITION_FACTS) values.remove(key);
        values.putAll(tracking.positionGeometry);
        if (!plugin.settings().enabled("settings.velocity-world-border")) values.put("source-world-border", 0d);
        if (world.geometryKnown()) {
            values.put("protocol-exempt-LIQUID", world.bubbleAge() < 25 ? 1d : 0d);
            values.put("protocol-exempt-SLIME", world.slimeBelowAge() < 65 || tracking.positionGeometry.getOrDefault("source-slime-contact", 0d) == 1 ? 1d : 0d);
            values.put("protocol-exempt-COLLIDING_VERTICALLY", world.ceilingAge() < 20 ? 1d : 0d);
            values.put("source-bubble-position-age", (double) world.bubbleAge());
            values.put("source-slime-below-position-age", (double) world.slimeBelowAge());
            values.put("source-bubble-contact-position-age", (double) world.bubbleAge());
            values.put("protocol-exempt-BUBBLE_COLUMN", world.bubbleAge() < 40 ? 1d : 0d);
            var boots = player.getInventory().getBoots();
            values.put("protocol-exempt-DEPTH_STRIDER", boots != null && boots.containsEnchantment(Enchantment.DEPTH_STRIDER) && world.bubbleAge() < 30 ? 1d : 0d);
        } else for (String key : List.of("protocol-exempt-LIQUID", "protocol-exempt-SLIME", "protocol-exempt-COLLIDING_VERTICALLY", "source-bubble-position-age", "source-slime-below-position-age", "source-bubble-contact-position-age", "protocol-exempt-BUBBLE_COLUMN", "protocol-exempt-DEPTH_STRIDER")) values.remove(key);
        if (world.initialStateKnown()) {
            values.put("protocol-exempt-LIQUID", world.bubbleAge() < 25 ? 1d : 0d);
            values.put("source-bubble-position-age", (double) world.bubbleAge());
        }
        if (world.waterloggedKnown()) {
            values.put("protocol-exempt-WATERLOGGED", world.waterloggedAge() < 30 ? 1d : 0d);
            values.put("source-waterlogged-position-age", (double) world.waterloggedAge());
        } else { values.remove("protocol-exempt-WATERLOGGED"); values.remove("source-waterlogged-position-age"); }
        for (String key : List.of("source-ice-position-age", "source-trapdoor-position-age", "source-bed-position-age", "source-fence-position-age", "source-soul-speed-position-age", "source-body-slime-position-age", "source-body-slab-position-age", "source-nearby-living-position-age")) values.remove(key);
        for (String key : List.of("protocol-exempt-HONEY", "protocol-exempt-ICE", "protocol-exempt-TRAPDOOR", "protocol-exempt-FENCE", "protocol-exempt-BED", "protocol-exempt-SHULKER_BOX", "protocol-exempt-SCAFFOLDING", "protocol-exempt-FARMLAND", "protocol-exempt-SHULKER", "protocol-exempt-HAPPY_GHAST", "protocol-exempt-PISTON", "source-ice-contact-position-age")) values.remove(key);
        if (world.contactsKnown()) {
            values.put("source-ice-position-age", (double) world.iceAge()); values.put("source-trapdoor-position-age", (double) world.trapdoorAge());
            values.put("source-bed-position-age", (double) world.bedAge()); values.put("source-fence-position-age", (double) world.fenceAge());
            values.put("source-soul-speed-position-age", (double) world.soulSpeedAge()); values.put("source-body-slime-position-age", (double) world.bodySlimeAge());
            values.put("source-body-slab-position-age", (double) world.bodySlabAge()); values.put("source-nearby-living-position-age", (double) world.nearbyLivingAge());
            values.put("source-ice-contact-position-age", (double) world.iceContactAge());
            values.put("protocol-exempt-HONEY", world.honeyAge() < 30 || tracking.positionGeometry.get("source-honey-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-ICE", world.iceContactAge() < 20 || tracking.positionGeometry.get("source-ice-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-TRAPDOOR", world.trapdoorAge() < 20 || tracking.positionGeometry.get("source-trapdoor-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-FENCE", world.fenceAge() < 7 || tracking.positionGeometry.get("source-fence-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-BED", world.bedAge() < 30 || tracking.positionGeometry.get("source-bed-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-SHULKER_BOX", world.shulkerBoxAge() < 60 || tracking.positionGeometry.get("source-shulker-box-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-SCAFFOLDING", world.scaffoldingAge() < 30 || tracking.positionGeometry.get("source-scaffolding-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-FARMLAND", world.farmlandAge() < 20 ? 1d : 0d);
            values.put("protocol-exempt-SHULKER", world.shulkerAge() < 40 || tracking.positionGeometry.get("source-nearby-shulker-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-HAPPY_GHAST", world.happyGhastAge() < 25 || tracking.positionGeometry.get("source-nearby-happy-ghast-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-PISTON", world.pistonAge() < 20 || tracking.positionGeometry.get("source-piston-contact") == 1 ? 1d : 0d);
            values.put("protocol-exempt-ENTITY_COLLISION", plugin.settings().enabled("settings.entity-collision") && world.nearbyLivingAge() < 8 ? 1d : 0d);
        } else values.remove("protocol-exempt-ENTITY_COLLISION");
        values.put("protocol-flight-cooldown", (double) plugin.settings().integer("settings.flight-cooldown"));
        if (position.flightKnown()) values.put("source-flight-position-age", (double) position.flightPositionAge()); else values.remove("source-flight-position-age");
        if (tracking.positionGeometry.containsKey("source-ceiling-obstructed")) values.put("source-ceiling-position-age", (double) tracking.ceilingPositionAge);
        else values.remove("source-ceiling-position-age");
        values.put("source-origin-skip", position.originSkip() ? 1d : 0d);
        if (position.positionKnown()) {
            values.put("source-position-x", position.rawX()); values.put("source-position-y", position.rawY()); values.put("source-position-z", position.rawZ());
            values.put("source-previous-position-x", position.previousX()); values.put("source-previous-position-z", position.previousZ());
            values.put("source-position-dx", position.dx()); values.put("source-position-dy", position.signedDy()); values.put("source-position-dz", position.dz());
            values.put("source-position-horizontal", Math.sqrt(position.dx() * position.dx() + position.dz() * position.dz()));
            values.put("source-delta-y", position.signedDy()); values.put("source-horizontal", Math.sqrt(position.dx() * position.dx() + position.dz() * position.dz()));
            values.put("source-previous-position-dx", position.previousDx()); values.put("source-previous-position-dy", position.previousDy()); values.put("source-previous-position-dz", position.previousDz());
            values.put("source-previous-position-horizontal", Math.sqrt(position.previousDx() * position.previousDx() + position.previousDz() * position.previousDz()));
            values.put("source-previous-horizontal", Math.sqrt(position.previousDx() * position.previousDx() + position.previousDz() * position.previousDz()));
            states.add("source-killaura-c-known");
            if (tracking.positionGeometry.containsKey("source-soul-sand")) {
                states.add("source-killaura-b-known"); states.add("source-killaura-l-known");
                if (tracking.positionGeometry.get("source-soul-sand") == 1) { states.add("source-killaura-b-exempt"); states.add("source-killaura-l-exempt"); }
                else { states.remove("source-killaura-b-exempt"); states.remove("source-killaura-l-exempt"); }
            } else { states.remove("source-killaura-b-known"); states.remove("source-killaura-l-known"); }
        }
        values.put("timer-origin-skip", position.originSkip() ? 1d : 0d);
        if (position.webKnown()) values.put("timer-web-exempt", position.webExempt() ? 1d : 0d);
        else values.remove("timer-web-exempt");
        values.put("timer-web-position-age", (double) position.webPositionAge());
        values.put("timer-signed-position-dy", position.signedDy());
        values.put("vehicle-position-age", (double) position.vehiclePositionAge());
        values.put("timer-vehicle-exempt", position.vehiclePositionAge() < 25 || player.isInsideVehicle() || player.getVehicle() != null ? 1d : 0d);
        double serverTps = plugin.tps();
        if (Double.isFinite(serverTps)) values.put("timer-tps", serverTps);
        else values.remove("timer-tps");
        var client = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (client != null && client.getClientVersion() != null && client.getClientVersion() != ClientVersion.UNKNOWN) {
            values.put("client-at-least-1-9", client.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? 1d : 0d);
            values.put("client-at-least-1-17", client.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17) ? 1d : 0d);
            values.put("client-at-least-1-21-2", client.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) ? 1d : 0d);
            values.put("client-newer-than-1-9", client.getClientVersion().isNewerThan(ClientVersion.V_1_9) ? 1d : 0d);
            values.put("client-at-least-1-13", client.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? 1d : 0d);
            values.put("client-at-least-1-15", client.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15) ? 1d : 0d);
        } else { for (String key : List.of("client-at-least-1-9", "client-at-least-1-17", "client-at-least-1-21-2", "client-newer-than-1-9", "client-at-least-1-13", "client-at-least-1-15")) values.remove(key); }
        values.put("cancelled-break-age-ticks", (double) tracking.clicks.cancelledBreakAge(plugin.tick()));
        values.put("teleport-movement-age", (double) tracking.teleportMovementAge);
        if (tracking.serverTeleportMatch) states.add("server-teleport-match"); else states.remove("server-teleport-match");
        if (tracking.bukkitTeleportMoves < 10 || tracking.serverTeleportMatch) states.add("teleport-exemption"); else states.remove("teleport-exemption");
        if (position.vehiclePositionAge() < 25) states.add("vehicle-grace"); else states.remove("vehicle-grace");
        if (tracking.positionGeometry.getOrDefault("source-nearby-boat-contact", 0d) == 1) states.add("nearby-boat"); else states.remove("nearby-boat");
        values.remove("protocol-exempt-BOAT");
        if (tracking.positionGeometry.containsKey("source-nearby-boat-contact")) values.put("protocol-exempt-BOAT", tracking.positionGeometry.get("source-nearby-boat-contact"));
        else if (world.initialStateKnown()) values.put("protocol-exempt-BOAT", 0d);
        values.put("protocol-exempt-CHUNK", !player.getWorld().isChunkLoaded((int) Math.floor(position.rawX()) >> 4, (int) Math.floor(position.rawZ()) >> 4) ? 1d : 0d);
        values.put("accepted-attack-age-ticks", (double) Math.max(0, plugin.tick() - tracking.acceptedAttackTick));
        if (tracking.events.age(Age.DEATH) < 25) states.add("death-grace"); else states.remove("death-grace");
        if (tracking.events.age(Age.WORLD_CHANGE) < 40) states.add("world-change-grace"); else states.remove("world-change-grace");
        values.put("original-delta-yaw", (double) camera.yawDelta());
        values.put("original-delta-pitch", (double) camera.pitchDelta());
        values.put("original-previous-delta-yaw", (double) camera.previousYawDelta());
        values.put("original-previous-delta-pitch", (double) camera.previousPitchDelta());
        values.put("original-signed-delta-pitch", (double) camera.signedPitchDelta());
        values.put("yaw-smoothing-residual", (double) camera.yawSmoothingResidual());
        values.put("previous-yaw-smoothing-residual", (double) camera.previousYawSmoothingResidual());
        values.put("sensitivity-mode", (double) camera.sensitivityMode());
        values.put("normalized-sensitivity", camera.normalizedSensitivity());
        values.put("mouse-step", (double) camera.mouseStep());
        values.put("mouse-yaw-count", (double) camera.yawCount());
        values.put("mouse-pitch-count", (double) camera.pitchCount());
    }

    private void protocolStates(Player player, Set<String> states) {
        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null || user.getClientVersion() == null || user.getClientVersion() == ClientVersion.UNKNOWN) return;
        ClientVersion version = user.getClientVersion();
        if (version.isOlderThan(ClientVersion.V_1_9)) states.add("legacy-client");
        if (version.isOlderThan(ClientVersion.V_1_21_2)) states.add("legacy-swing-order");
        else states.add("client-tick-end-supported");
    }

    private void velocityModel(Tracking t, Map<String, Double> values, Set<String> states, double yaw) {
        if (!t.actualVelocitySeen || !states.contains("velocity-physics-known") || !states.contains("unobstructed")
                || !values.keySet().containsAll(List.of("movement-speed","friction","gravity","jump-strength"))) {
            t.velocityModel.invalidate();
            return;
        }
        var input = new HorizontalVelocityModel.Input(t.inputKnown, t.forwardInput, t.strafeInput, yaw,
                values.get("movement-speed"), values.get("friction"), values.get("gravity"), values.get("jump-strength") + values.getOrDefault("jump-effect", 0d) * .1,
                states.contains("ground"), states.contains("sprinting"), t.inputJump, t.attacksSinceMove > 0);
        t.velocityModel.movement(input).ifPresent(bounds -> {
            states.add("velocity-model-known");
            values.put("predicted-horizontal-min", bounds.minimum()); values.put("predicted-horizontal-max", bounds.maximum());
            values.put("predicted-vertical-min", bounds.verticalMinimum()); values.put("predicted-vertical-max", bounds.verticalMaximum());
            values.put("predicted-projection-min", bounds.projectionMinimum()); values.put("predicted-projection-max", bounds.projectionMaximum());
        });
    }

    private boolean loaded(Player player, int x, int y, int z) {
        return y >= player.getWorld().getMinHeight() && y < player.getWorld().getMaxHeight() && player.getWorld().isChunkLoaded(x >> 4, z >> 4);
    }

    private Boolean obstructed(Player player, Location target) {
        Location origin = player.getEyeLocation();
        Vector delta = target.toVector().subtract(origin.toVector());
        double distance = delta.length();
        if (distance < .1 || distance > 12) return null;
        Vector unit = delta.clone().normalize();
        for (double travelled = 0; travelled <= distance; travelled += .5) {
            Location sample = origin.clone().add(unit.clone().multiply(travelled));
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (!player.getWorld().isChunkLoaded((sample.getBlockX() + x) >> 4, (sample.getBlockZ() + z) >> 4)) return null;
            }
        }
        var hit = player.getWorld().rayTraceBlocks(origin, unit, distance, org.bukkit.FluidCollisionMode.NEVER, true);
        return hit != null && hit.getHitBlock() != null && !hit.getHitBlock().equals(target.getBlock())
                && hit.getHitPosition().distance(origin.toVector()) < distance - .8;
    }

    private int effect(Player player, PotionEffectType type) {
        var effect = player.getPotionEffect(type); return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private double stableQuantum(ArrayDeque<Double> window, double value) {
        if (value > 1e-7 && value < 1) {
            window.addLast(Math.rint(value * 100000) / 100000);
            if (window.size() > 40) window.removeFirst();
        }
        Map<Double, Integer> counts = new HashMap<>();
        window.forEach(sample -> counts.merge(sample, 1, Integer::sum));
        return counts.entrySet().stream().filter(entry -> entry.getKey() > 0 && entry.getValue() >= 20)
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0d);
    }

    private static final class Tracking {
        private Location position, serverLocation, serverDestination, vehicleLocation;
        private UUID vehicleId;
        private Map<String, Double> context = Map.of();
        private Map<String, Double> lastMotion = Map.of();
        private Set<String> states = Set.of();
        private Vector velocity = new Vector();
        private final HorizontalVelocityModel velocityModel = new HorizontalVelocityModel();
        private final CinematicState cinematic = new CinematicState();
        private final PositionState positionState = new PositionState();
        private final io.github.lino9999.linoac.detection.collector.CachedWorldState cachedWorld = new io.github.lino9999.linoac.detection.collector.CachedWorldState();
        private long worldGeneration;
        private final Map<String, Double> positionGeometry = new HashMap<>();
        private int ceilingPositionAge = 100, ghostCorrectionMoves, ghostCorrections;
        private final ClickContext clicks = new ClickContext(System.currentTimeMillis());
        private final SourceEventState events = new SourceEventState();
        private io.github.lino9999.linoac.detection.collector.SourceAbilityState abilities = new io.github.lino9999.linoac.detection.collector.SourceAbilityState();
        private final io.github.lino9999.linoac.detection.world.MiningContext mining = new io.github.lino9999.linoac.detection.world.MiningContext();
        private final io.github.lino9999.linoac.detection.player.ProtocolContext protocolContext = new io.github.lino9999.linoac.detection.player.ProtocolContext(System.currentTimeMillis());
        private final io.github.lino9999.linoac.detection.combat.SequenceContext sequenceContext = new io.github.lino9999.linoac.detection.combat.SequenceContext();
        private final io.github.lino9999.linoac.detection.combat.CombatExemptions combatExemptions = new io.github.lino9999.linoac.detection.combat.CombatExemptions();
        private final io.github.lino9999.linoac.detection.combat.TargetHistory targetHistory = new io.github.lino9999.linoac.detection.combat.TargetHistory();
        private io.github.lino9999.linoac.detection.combat.VelocityContext sourceVelocity = new io.github.lino9999.linoac.detection.combat.VelocityContext();
        private io.github.lino9999.linoac.detection.combat.VelocityPrediction velocityPrediction = new io.github.lino9999.linoac.detection.combat.VelocityPrediction();
        private final Map<String, Double> acknowledgedState = new HashMap<>();
        private int attackMovementAge = 100, teleportMovementAge = 100;
        private int totalMoves;
        private int bukkitTeleportMoves = 100;
        private boolean serverNearbyFirework, serverTeleportMatch;
        private io.github.lino9999.linoac.packet.ServerWorldSnapshot serverWorld = io.github.lino9999.linoac.packet.ServerWorldSnapshot.unknown(-1);
        private long acceptedAttackTick = -100;
        private int serverNearbyEntities = -1;
        private int mirroredNearbyEntities = -1;
        private int mirroredNearbyBoats = -1;
        private final ArrayDeque<Double> yawQuanta = new ArrayDeque<>(), pitchQuanta = new ArrayDeque<>();
        private int sensitivitySamples;
        private boolean inputKnown;
        private double strafeInput, forwardInput, serverHorizontal, oldestTransactionMs;
        private double dx, dy, dz, deltaYaw, deltaPitch, vehicleDy, transactionPing;
        private int groundTicks, airTicks, glideTicks, vehicleAirTicks, targetId = -1, slot = -1;
        private int packetsSinceMove, placementsSinceMove, digsSinceMove, actionsSinceMove, attacksSinceMove, slotsSinceMove, attacksSinceSwing, swingsSinceAttack, pendingTransactions, ghostSetbacks;
        private long attackTick = -1000, swingTick = -1000, velocityTick = -1000, teleportTick, vehicleSteeredTick = -1000;
        private long moveNanos, swingNanos, lastPacketNanos, digStarted, elytraToggleNanos, actualVelocityNanos, lastAckNanos;
        private long serverSampleTick = -1000, fireworkUntil, vehicleSpecialUntil;
        private long placeTick = -1000, breakTick = -1000;
        private boolean serverGround, clientGround, previousClientGround, digging, velocityConfirmed, actualVelocitySeen, inputJump, connectionTracked, acknowledgementSeen;
        private boolean sourceSprinting, sourceSneaking, sourcePreviousPreviousGround, cachedBootsSoulSpeed, cachedChestElytra;
        private boolean transportComplete = true;
        private String previousType;
        private Tracking(Location location) { position = location.clone(); serverLocation = location.clone(); }
    }
}
