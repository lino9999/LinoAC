package io.github.lino9999.linoac.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import io.github.lino9999.linoac.LinoAC;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.player.PlayerData;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolMonitor extends PacketListenerAbstract {
    private final LinoAC plugin;
    private final WireDecoder decoder = new WireDecoder();
    private final Map<UUID, PacketBuffer> buffers = new ConcurrentHashMap<>();
    private final Map<UUID, RateWindow> rates = new HashMap<>();
    private final Map<UUID, Integer> playerEntities = new ConcurrentHashMap<>();
    private final Map<Integer, RawPosition> rawPositions = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger nextProbe = new java.util.concurrent.atomic.AtomicInteger();
    private final Map<UUID, Integer> selectedTargets = new ConcurrentHashMap<>();

    public ProtocolMonitor(LinoAC plugin) { super(PacketListenerPriority.NORMAL); this.plugin = plugin; }
    public void add(Player player) { plugin.collector().registerJoin(player); buffers.computeIfAbsent(player.getUniqueId(), ignored -> new PacketBuffer()); rates.remove(player.getUniqueId()); playerEntities.put(player.getUniqueId(), player.getEntityId()); }
    public void remove(UUID id) { buffers.remove(id); rates.remove(id); selectedTargets.remove(id); Integer entity = playerEntities.remove(id); if (entity != null) rawPositions.remove(entity); }
    public void clear() { buffers.clear(); rates.clear(); playerEntities.clear(); rawPositions.clear(); selectedTargets.clear(); }
    public void serverNearby(Player player, int count) {
        PacketBuffer buffer = buffers.get(player.getUniqueId());
        if (buffer != null) buffer.serverNearby(count);
    }
    public void serverWorld(Player player, ServerWorldSnapshot snapshot) {
        PacketBuffer buffer = buffers.get(player.getUniqueId());
        if (buffer != null) buffer.serverWorld(snapshot);
    }

    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID id = event.getUser().getUUID();
        if (id != null) { buffers.remove(id); selectedTargets.remove(id); Integer entity = playerEntities.remove(id); if (entity != null) rawPositions.remove(entity); }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        UUID id = event.getUser().getUUID();
        if (id == null || event.isCancelled()) return;
        PacketBuffer buffer = buffers.get(id);
        if (buffer == null) return;
        try {
            buffer.sulfurCapability(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_26_1));
            WirePacket packet = decoder.decode(event, buffer);
            var measurements = new HashMap<>(packet.values());
            measurements.put("observation-server-tick", (double) plugin.tick());
            packet = new WirePacket(packet.kind(), packet.type(), packet.nanos(), packet.token(), measurements, packet.states());
            if (packet.kind() == Observation.Kind.ATTACK && packet.values().containsKey("target-id")) {
                int target = packet.values().get("target-id").intValue();
                if (rawPositions.containsKey(target)) selectedTargets.put(id, target);
            }
            if (packet.kind() == Observation.Kind.MOVE && packet.state("position-packet") && !packet.state("nonfinite-position") && !packet.state("outside-world") && !packet.state("invalid-pitch") && !packet.state("nonfinite-rotation")
                    && packet.values().keySet().containsAll(Set.of("x", "y", "z"))) {
                Integer entity = playerEntities.get(id);
                if (entity != null) rawPositions.put(entity, new RawPosition(packet.values().get("x"), packet.values().get("y"), packet.values().get("z")));
            }
            buffer.add(packet);
        }
        catch (RuntimeException e) {
            event.setCancelled(true);
            buffer.add(new WirePacket(Observation.Kind.PACKET, "MALFORMED", System.nanoTime(), 0, Map.of(), Set.of("malformed-packet")));
        }
    }

    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled() || event.getUser() == null || event.getUser().getUUID() == null) return;
        PacketBuffer buffer = buffers.computeIfAbsent(event.getUser().getUUID(), ignored -> new PacketBuffer());
        String type = event.getPacketType().toString();
        boolean sulfurSupported = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_26_1);
        buffer.sulfurCapability(sulfurSupported);
        if (sulfurSupported) captureSulfur(event, buffer, type);
        long now = System.nanoTime();
        switch (type) {
            case "PLAYER_ABILITIES" -> buffer.ability(new WrapperPlayServerPlayerAbilities(event).isFlightAllowed());
            case "CHANGE_GAME_STATE" -> {
                var packet = new WrapperPlayServerChangeGameState(event);
                if (packet.getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE && Float.isFinite(packet.getValue()) && packet.getValue() == Math.rint(packet.getValue())) buffer.gameMode((int) packet.getValue());
            }
            case "ENTITY_METADATA" -> {
                var packet = new WrapperPlayServerEntityMetadata(event);
                if (packet.getEntityId() == event.getUser().getEntityId() && event.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
                    Boolean gliding = null, riptide = null, swimming = null;
                    for (var data : packet.getEntityMetadata()) if (data.getValue() instanceof Byte flags) {
                        if (data.getIndex() == 0) { gliding = (flags & 0x80) != 0; swimming = (flags & 0x10) != 0; }
                        if (data.getIndex() == 8) riptide = (flags & 4) != 0;
                    }
                    buffer.metadata(gliding, riptide, swimming);
                }
            }
            case "UPDATE_ATTRIBUTES" -> {
                var packet = new WrapperPlayServerUpdateAttributes(event);
                if (packet.getEntityId() == event.getUser().getEntityId()) for (var property : packet.getProperties()) {
                    String key = property.getKey().toLowerCase(java.util.Locale.ROOT);
                    if (key.endsWith("movement_speed") || key.endsWith("movement.speed")) buffer.attribute("protocol-movement-speed", property.calcValue());
                    if (key.endsWith("entity_interaction_range") || key.endsWith("entity.interaction.range")) buffer.attribute("protocol-attack-range", attributeValue(property));
                    if (key.endsWith("jump_strength") || key.endsWith("jump.strength")) buffer.attribute("protocol-jump-strength", attributeValue(property));
                    if (key.endsWith("step_height") || key.endsWith("step.height")) buffer.attribute("protocol-step-height", attributeValue(property));
                    if (key.endsWith("gravity")) buffer.attribute("protocol-gravity", attributeValue(property));
                    if (key.endsWith("air_drag_modifier")) buffer.attribute("protocol-air-drag", attributeValue(property));
                    if (key.endsWith("bounciness")) buffer.attribute("protocol-bounciness", attributeValue(property));
                    if (key.endsWith("friction_modifier")) buffer.attribute("protocol-friction-modifier", attributeValue(property));
                }
            }
            case "ENTITY_EFFECT" -> {
                var packet = new WrapperPlayServerEntityEffect(event);
                if (packet.getEntityId() == event.getUser().getEntityId()) {
                    String key = effectKey(packet.getPotionType());
                    if (key != null) buffer.effect(key, Math.max(0, packet.getEffectAmplifier() + 1));
                }
            }
            case "REMOVE_ENTITY_EFFECT" -> {
                var packet = new WrapperPlayServerRemoveEntityEffect(event);
                if (packet.getEntityId() == event.getUser().getEntityId()) {
                    String key = effectKey(packet.getPotionType());
                    if (key != null) buffer.effect(key, 0);
                }
            }
            case "SPAWN_ENTITY" -> {
                var packet = new WrapperPlayServerSpawnEntity(event); var position = packet.getPosition();
                buffer.entity(spawn(packet.getEntityId(), position, packet.getEntityType()));
            }
            case "SPAWN_LIVING_ENTITY" -> {
                var packet = new WrapperPlayServerSpawnLivingEntity(event); var position = packet.getPosition();
                buffer.entity(spawn(packet.getEntityId(), position, packet.getEntityType()));
            }
            case "SPAWN_PLAYER" -> {
                var packet = new WrapperPlayServerSpawnPlayer(event); var position = packet.getPosition();
                buffer.entity(EntityPositionTracker.Update.spawn(packet.getEntityId(), position.getX(), position.getY(), position.getZ(), "PLAYER", true));
            }
            case "DESTROY_ENTITIES" -> {
                for (int id : new WrapperPlayServerDestroyEntities(event).getEntityIds()) buffer.entity(EntityPositionTracker.Update.destroy(id));
            }
            case "ENTITY_RELATIVE_MOVE" -> {
                var packet = new WrapperPlayServerEntityRelativeMove(event);
                buffer.entity(EntityPositionTracker.Update.move(packet.getEntityId(), packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ(), true));
                relativeTarget(buffer, event.getUser().getUUID(), packet.getEntityId(), packet.getDeltaX(), packet.getDeltaZ(), now);
            }
            case "ENTITY_RELATIVE_MOVE_AND_ROTATION" -> {
                var packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
                buffer.entity(EntityPositionTracker.Update.move(packet.getEntityId(), packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ(), true));
                relativeTarget(buffer, event.getUser().getUUID(), packet.getEntityId(), packet.getDeltaX(), packet.getDeltaZ(), now);
            }
            case "ENTITY_TELEPORT" -> {
                var packet = new WrapperPlayServerEntityTeleport(event); var position = packet.getPosition();
                buffer.entity(EntityPositionTracker.Update.move(packet.getEntityId(), position.getX(), position.getY(), position.getZ(), false));
            }
            case "ENTITY_POSITION_SYNC" -> {
                var packet = new WrapperPlayServerEntityPositionSync(event); var position = packet.getPosition().getEndPosition();
                buffer.entity(EntityPositionTracker.Update.move(packet.getId(), position.getX(), position.getY(), position.getZ(), false));
            }
            case "RESPAWN" -> { buffer.resetEntities(); rawPositions.remove(event.getUser().getEntityId()); }
            case "SET_PASSENGERS" -> {
                var packet = new WrapperPlayServerSetPassengers(event);
                buffer.passengers(packet.getEntityId(), packet.getPassengers(), event.getUser().getEntityId());
            }
            case "PLAYER_POSITION_AND_LOOK" -> {
                var packet = new WrapperPlayServerPlayerPositionAndLook(event);
                buffer.playerPosition(packet.getX(), packet.getY(), packet.getZ(), packet.getRelativeFlags().getFullMask());
                buffer.add(new WirePacket(Observation.Kind.SERVER_POSITION, "SERVER_POSITION_AND_LOOK", now, 0,
                        Map.of("packet-time-ms", (double) System.currentTimeMillis(), "server-at-least-1-9", 1d), Set.of("packet:SERVER_POSITION_AND_LOOK")));
            }
            default -> { }
        }
        if (type.equals("PING")) {
            int id = new WrapperPlayServerPing(event).getId();
            if ((id & 0xFF000000) == 0xC1000000) buffer.expect(id, now, true);
        }
        else if (type.equals("KEEP_ALIVE")) buffer.expect(new WrapperPlayServerKeepAlive(event).getId(), now, false);
        else if (type.equals("ENTITY_VELOCITY")) {
            var packet = new WrapperPlayServerEntityVelocity(event);
            if (packet.getEntityId() == event.getUser().getEntityId()) {
                var velocity = packet.getVelocity();
                buffer.bracketImpulse(false, velocity.getX(), velocity.getY(), velocity.getZ());
                event.getTasksAfterSend().add(() -> sendProbe(event.getUser(), buffer));
                buffer.add(new WirePacket(Observation.Kind.VELOCITY, "SERVER_VELOCITY", now, 0,
                        Map.of("velocity-x", velocity.getX(), "velocity-y", velocity.getY(), "velocity-z", velocity.getZ()), Set.of()));
            }
        }
        else if (type.equals("EXPLOSION")) {
            var packet = new WrapperPlayServerExplosion(event);
            var impulse = packet.getKnockback();
            var motion = packet.getPlayerMotion();
            if (impulse != null && motion != null && (motion.getX() != 0 || motion.getY() != 0 || motion.getZ() != 0)) {
                buffer.bracketImpulse(true, impulse.getX(), impulse.getY(), impulse.getZ());
                event.getTasksAfterSend().add(() -> sendProbe(event.getUser(), buffer));
            }
        }
    }

    private static double attributeValue(WrapperPlayServerUpdateAttributes.Property property) {
        var adjustments = property.getModifiers().stream().map(modifier -> new io.github.lino9999.linoac.detection.collector.AttributeMath.Adjustment(
                switch (modifier.getOperation()) {
                    case ADDITION -> io.github.lino9999.linoac.detection.collector.AttributeMath.Operation.ADD;
                    case MULTIPLY_BASE -> io.github.lino9999.linoac.detection.collector.AttributeMath.Operation.SCALE_BASE;
                    case MULTIPLY_TOTAL -> io.github.lino9999.linoac.detection.collector.AttributeMath.Operation.SCALE_TOTAL;
                }, modifier.getAmount())).toList();
        return io.github.lino9999.linoac.detection.collector.AttributeMath.evaluate(property.getValue(), adjustments);
    }

    private static void captureSulfur(PacketSendEvent event, PacketBuffer buffer, String type) {
        try {
            switch (type) {
                case "CHUNK_DATA" -> {
                    var packet = new WrapperPlayServerChunkData(event);
                    var dimension = event.getUser().getDimensionType();
                    if (dimension == null) { buffer.invalidateSulfur(); return; }
                    var captured = SulfurPackets.capture(packet.getColumn(), dimension.getMinY(), dimension.getHeight(), packet.getServerVersion().toClientVersion());
                    event.getTasksAfterSend().add(() -> buffer.sulfurChunk(captured));
                }
                case "BLOCK_CHANGE" -> {
                    var packet = new WrapperPlayServerBlockChange(event); var position = packet.getBlockPosition();
                    var captured = new SparseBlockMirror.Block(position.getX(), position.getY(), position.getZ());
                    boolean present = packet.getBlockState().getType() == com.github.retrooper.packetevents.protocol.world.states.type.StateTypes.POTENT_SULFUR;
                    event.getTasksAfterSend().add(() -> buffer.sulfurBlock(captured, present));
                }
                case "MULTI_BLOCK_CHANGE" -> {
                    var packet = new WrapperPlayServerMultiBlockChange(event);
                    var changes = new java.util.ArrayList<SulfurPackets.Change>();
                    for (var block : packet.getBlocks()) changes.add(new SulfurPackets.Change(new SparseBlockMirror.Block(block.getX(), block.getY(), block.getZ()), block.getBlockState(packet.getServerVersion().toClientVersion()).getType() == com.github.retrooper.packetevents.protocol.world.states.type.StateTypes.POTENT_SULFUR));
                    var captured = java.util.List.copyOf(changes);
                    event.getTasksAfterSend().add(() -> buffer.sulfurBlocks(captured));
                }
                case "UNLOAD_CHUNK" -> {
                    var packet = new WrapperPlayServerUnloadChunk(event);
                    int x = packet.getChunkX(), z = packet.getChunkZ();
                    event.getTasksAfterSend().add(() -> buffer.sulfurUnload(x, z));
                }
                default -> { }
            }
        } catch (RuntimeException failure) { buffer.invalidateSulfur(); }
    }

    private static String effectKey(com.github.retrooper.packetevents.protocol.potion.PotionType type) {
        if (type == null) return null;
        return switch (type.getName().getKey()) {
            case "levitation" -> "protocol-levitation-amplifier";
            case "slowness" -> "protocol-slowness";
            case "jump_boost" -> "protocol-jump-amplifier";
            case "conduit_power" -> "protocol-conduit-power";
            case "speed" -> "protocol-speed-amplifier";
            case "slow_falling" -> "protocol-slow-falling";
            default -> null;
        };
    }

    private void relativeTarget(PacketBuffer buffer, UUID observer, int entityId, double deltaX, double deltaZ, long nanos) {
        if (!Integer.valueOf(entityId).equals(selectedTargets.get(observer))) return;
        RawPosition target = rawPositions.get(entityId);
        if (target == null) return;
        buffer.add(new WirePacket(Observation.Kind.SERVER_POSITION, "SERVER_RELATIVE_TARGET", nanos, 0,
                Map.of("target-id", (double) entityId, "target-raw-x", target.x(), "target-raw-z", target.z(),
                        "target-delta-x", deltaX, "target-delta-z", deltaZ, "history-server-tick", (double) plugin.tick()), Set.of()));
    }

    private record RawPosition(double x, double y, double z) {}

    private static EntityPositionTracker.Update spawn(int id, com.github.retrooper.packetevents.util.Vector3d position,
                                                     com.github.retrooper.packetevents.protocol.entity.type.EntityType type) {
        return new EntityPositionTracker.Update(id, position.getX(), position.getY(), position.getZ(), EntityPositionTracker.UpdateKind.SPAWN,
                type == null ? "UNKNOWN" : type.getName().toString(), type == null ? null : type.isInstanceOf(com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.LIVINGENTITY));
    }

    private void sendProbe(com.github.retrooper.packetevents.protocol.player.User user, PacketBuffer buffer) {
        if (user.getClientVersion().isOlderThan(ClientVersion.V_1_17) || buffer.pendingPings() >= 256) return;
        user.sendPacket(new WrapperPlayServerPing(0xC1000000 | (nextProbe.getAndIncrement() & 0x00FFFFFF)));
    }

    public void tick(Player player, PlayerData data) {
        PacketBuffer buffer = buffers.get(data.id);
        if (buffer == null) return;
        long now = System.nanoTime();
        PacketBuffer.Batch batch = buffer.drain();
        RateWindow rate = rates.computeIfAbsent(data.id, ignored -> new RateWindow(now));
        rate.packets += batch.received();
        boolean malformed = false;
        for (WirePacket packet : batch.packets()) {
            if (packet.kind() == Observation.Kind.SWING) rate.swings++;
            if (packet.states().contains("malformed-packet")) malformed = true;
            plugin.collector().accept(player, packet);
        }
        double seconds = (now - rate.started) / 1_000_000_000d;
        if (seconds >= 5) {
            data.swings = (int) Math.round(rate.swings / seconds);
            if (plugin.settings().enabled("connection.flood-kick") && rate.packets / seconds > plugin.settings().number("limits.maximum-packets")) malformed = true;
            rates.put(data.id, new RateWindow(now));
        }
        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        boolean tracked = user != null && user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17);
        long oldest = buffer.oldestPendingNanos();
        double ageMs = oldest == 0 ? 0 : (now - oldest) / 1_000_000d;
        plugin.collector().connection(player, tracked ? buffer.pendingPings() : 0, ageMs, tracked);
        boolean timeout = tracked && (buffer.acknowledgementOverflow() || ageMs > plugin.settings().integer("connection.timeout-seconds") * 1000d
                || buffer.pendingPings() >= plugin.settings().integer("connection.maximum-pending")
                || player.getPing() > plugin.settings().integer("connection.maximum-ping"));
        if (plugin.settings().enabled("connection.enabled") && (timeout || malformed || batch.dropped() > 0 && plugin.settings().enabled("connection.flood-kick"))) {
            player.kickPlayer(plugin.messages().color(plugin.settings().text("connection.kick-message")));
            return;
        }
        if (tracked && plugin.tick() % 10 == 0 && buffer.pendingPings() < 256) {
            sendProbe(user, buffer);
        }
    }

    public String clientVersion(Player player) {
        var user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        return user == null ? "unknown" : user.getClientVersion().getReleaseName();
    }

    private static final class RateWindow {
        private final long started;
        private int packets, swings;
        private RateWindow(long started) { this.started = started; }
    }
}
