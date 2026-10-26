package io.github.lino9999.linoac.packet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public final class PacketBuffer {
    private final ArrayDeque<WirePacket> packets = new ArrayDeque<>();
    private final Map<Long, Long> keepAlives = new LinkedHashMap<>();
    private final Map<Long, Long> pings = new LinkedHashMap<>();
    private final java.util.LinkedHashSet<Long> completedKeepAlives = new java.util.LinkedHashSet<>();
    private final EntityPositionTracker entities = new EntityPositionTracker(16384);
    private final io.github.lino9999.linoac.detection.collector.TeleportState teleports = new io.github.lino9999.linoac.detection.collector.TeleportState();
    private final Map<Long, java.util.ArrayList<Runnable>> acknowledgedUpdates = new LinkedHashMap<>();
    private final java.util.ArrayList<Runnable> awaitingProbe = new java.util.ArrayList<>();
    private final Map<String, Double> acknowledgedState = new HashMap<>(Map.ofEntries(Map.entry("protocol-flight-allowed", 0d),
            Map.entry("protocol-movement-speed", .1), Map.entry("protocol-attack-range", 3d), Map.entry("protocol-levitation-amplifier", 0d),
            Map.entry("protocol-jump-amplifier", 0d), Map.entry("protocol-conduit-power", 0d), Map.entry("protocol-speed-amplifier", 0d),
            Map.entry("protocol-jump-strength", .42), Map.entry("protocol-last-speed-amplifier", 0d), Map.entry("protocol-cached-creative", 0d), Map.entry("protocol-cached-spectator", 0d),
            Map.entry("protocol-slow-falling", 0d), Map.entry("protocol-metadata-gliding", 0d), Map.entry("protocol-gliding-revision", 0d), Map.entry("protocol-metadata-riptide", 0d), Map.entry("protocol-metadata-swimming", 0d)));
    private int pendingEntityUpdates;
    private int serverNearbyEntities = -1;
    private ServerWorldSnapshot serverWorld = ServerWorldSnapshot.unknown(-1);
    private double playerX, playerY, playerZ;
    private boolean positionKnown;
    private boolean acknowledgementOverflow;
    private boolean vehicleTypeKnown = true, vehicleTypePresent;
    private int occupiedVehicleId = -1;
    private int received;
    private int dropped;

    public synchronized void add(WirePacket packet) {
        boolean incoming = packet.kind() != io.github.lino9999.linoac.detection.Observation.Kind.VELOCITY && packet.kind() != io.github.lino9999.linoac.detection.Observation.Kind.SERVER_POSITION;
        if (incoming) received = Math.min(1_000_000, received + 1);
        if (incoming) {
            var values = new HashMap<>(packet.values());
            values.putAll(acknowledgedState);
            packet = new WirePacket(packet.kind(), packet.type(), packet.nanos(), packet.token(), values, packet.states());
        }
        if (incoming && vehicleTypeKnown) {
            var values = new HashMap<>(packet.values());
            values.put("timer-vehicle-type-present", vehicleTypePresent ? 1d : 0d);
            packet = new WirePacket(packet.kind(), packet.type(), packet.nanos(), packet.token(), values, packet.states());
        }
        if (packet.kind() == io.github.lino9999.linoac.detection.Observation.Kind.MOVE) {
            var values = new HashMap<>(packet.values());
            var states = new java.util.HashSet<>(packet.states());
            boolean corrupt = states.contains("nonfinite-position") || states.contains("nonfinite-rotation") || states.contains("invalid-pitch") || states.contains("outside-world");
            if (serverNearbyEntities >= 0) values.put("server-nearby-entities", (double) serverNearbyEntities);
            values.put("server-world-sample-known", serverWorld.complete() ? 1d : 0d);
            if (serverWorld.complete()) {
                values.put("server-world-generation", (double) serverWorld.generation());
                values.put("server-world-waterlogged", serverWorld.waterlogged() ? 1d : 0d);
                values.put("server-world-cobweb", serverWorld.cobweb() ? 1d : 0d);
                values.put("server-world-air-below", (double) serverWorld.airBelow());
                values.put("server-world-two-below-air", serverWorld.twoBelowAir() ? 1d : 0d);
                values.putAll(serverWorld.materialFacts());
                values.put("source-nearby-living-contact", serverWorld.nearby().living() ? 1d : 0d);
                values.put("source-nearby-boat-contact", serverWorld.nearby().boat() ? 1d : 0d);
                values.put("source-nearby-shulker-contact", serverWorld.nearby().shulker() ? 1d : 0d);
                values.put("source-nearby-happy-ghast-contact", serverWorld.nearby().happyGhast() ? 1d : 0d);
            }
            if (!corrupt && packet.states().contains("position-packet") && values.keySet().containsAll(java.util.Set.of("x", "y", "z"))) {
                playerX = values.get("x"); playerY = values.get("y"); playerZ = values.get("z"); positionKnown = true;
                int nearby = entities.nearby(values.get("x"), values.get("y"), values.get("z"));
                if (nearby >= 0) { values.put("mirrored-nearby-entities", (double) nearby); states.add("entity-mirror-known"); }
                int boats = entities.nearbyBoats(playerX, playerY, playerZ);
                if (boats >= 0) values.put("mirrored-nearby-boats", (double) boats);
                int living = entities.nearbyLiving(playerX, playerY, playerZ);
                if (living >= 0) values.put("mirrored-nearby-living", (double) living);
            }
            if (!corrupt) {
                var teleport = teleports.move(packet.state("position-packet") && positionKnown, packet.state("rotation-packet"), packet.state("client-ground"), playerX, playerY, playerZ);
                values.put("teleport-movement-age", (double) teleport.movementAge());
                if (teleport.matched()) states.add("server-teleport-match");
            }
            packet = new WirePacket(packet.kind(), packet.type(), packet.nanos(), packet.token(), values, states);
        }
        if (packets.size() == 512) { dropped = Math.min(1_000_000, dropped + 1); invalidateEntities(); return; }
        packets.addLast(packet);
    }

    public synchronized Batch drain() {
        Batch result = new Batch(List.copyOf(packets), received, dropped);
        packets.clear(); received = dropped = 0;
        return result;
    }

    public synchronized void expect(long token, long nanos, boolean ping) {
        Map<Long, Long> map = ping ? pings : keepAlives;
        if (map.size() == 256) {
            Long oldest = map.keySet().iterator().next();
            map.remove(oldest);
            if (ping) { acknowledgementOverflow = true; invalidateEntities(); }
        }
        map.put(token, nanos);
        if (ping && !awaitingProbe.isEmpty()) {
            acknowledgedUpdates.put(token, new java.util.ArrayList<>(awaitingProbe));
            awaitingProbe.clear();
        }
    }

    public synchronized Acknowledgement acknowledge(long token, long nanos, boolean ping) {
        Map<Long, Long> map = ping ? pings : keepAlives;
        boolean ordered = !map.isEmpty() && map.keySet().iterator().next() == token;
        if (ping && !ordered) return new Acknowledgement(false, false, false, -1);
        Long sent = map.remove(token);
        if (ping && sent != null) {
            var updates = acknowledgedUpdates.remove(token);
            if (updates != null) {
                pendingEntityUpdates -= updates.size();
                updates.forEach(Runnable::run);
            }
        }
        boolean duplicate = !ping && completedKeepAlives.contains(token);
        if (!ping && sent != null) {
            if (completedKeepAlives.size() >= 256) completedKeepAlives.remove(completedKeepAlives.iterator().next());
            completedKeepAlives.add(token);
        }
        return new Acknowledgement(sent != null, ordered, duplicate, sent == null ? -1 : Math.max(0, nanos - sent));
    }

    public synchronized int pendingPings() { return pings.size(); }
    public synchronized long oldestPendingNanos() { return pings.isEmpty() ? 0 : pings.values().iterator().next(); }

    public synchronized void entity(EntityPositionTracker.Update update) {
        queueAcknowledgement(() -> entities.apply(update));
    }

    public synchronized void bracketImpulse(boolean explosion, double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        if (explosion && (float) x == 0 && (float) y == 0 && (float) z == 0) return;
        String suffix = explosion ? "EXPLOSION" : "VELOCITY";
        Runnable begin = () -> add(new WirePacket(io.github.lino9999.linoac.detection.Observation.Kind.VELOCITY, "ACK_BEGIN_" + suffix,
                System.nanoTime(), 0, Map.of("velocity-x", x, "velocity-y", y, "velocity-z", z), java.util.Set.of()));
        if (pings.isEmpty()) begin.run();
        else {
            if (pendingEntityUpdates >= 8192) { invalidateEntities(); return; }
            Long latest = null;
            for (Long token : pings.keySet()) latest = token;
            acknowledgedUpdates.computeIfAbsent(latest, ignored -> new java.util.ArrayList<>()).add(begin);
            pendingEntityUpdates++;
        }
        queueAcknowledgement(() -> add(new WirePacket(io.github.lino9999.linoac.detection.Observation.Kind.VELOCITY, "ACK_END_" + suffix,
                System.nanoTime(), 0, Map.of(), java.util.Set.of())));
    }

    private void queueAcknowledgement(Runnable update) {
        if (pendingEntityUpdates >= 8192) { invalidateEntities(); return; }
        awaitingProbe.add(update);
        pendingEntityUpdates++;
    }

    public synchronized void playerPosition(double x, double y, double z, int relativeMask) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return;
        if ((relativeMask & 7) != 0 && !positionKnown) return;
        double targetX = (relativeMask & 1) != 0 ? playerX + x : x;
        double targetY = (relativeMask & 2) != 0 ? playerY + y : y;
        double targetZ = (relativeMask & 4) != 0 ? playerZ + z : z;
        if (!Double.isFinite(targetX) || !Double.isFinite(targetY) || !Double.isFinite(targetZ)) return;
        queueAcknowledgement(() -> teleports.acknowledgePosition(targetX, targetY, targetZ));
    }

    public synchronized void passengers(int vehicleId, int[] passengers, int selfEntityId) {
        if (selfEntityId < 0) return;
        boolean containsSelf = java.util.Arrays.stream(passengers).anyMatch(id -> id == selfEntityId);
        queueAcknowledgement(() -> {
            if (containsSelf) {
                occupiedVehicleId = vehicleId;
                var entity = entities.snapshot().get(vehicleId);
                vehicleTypeKnown = entities.known() && entity != null && !entity.type().equals("UNKNOWN");
                vehicleTypePresent = vehicleTypeKnown;
            } else if (occupiedVehicleId == vehicleId) {
                occupiedVehicleId = -1;
                vehicleTypeKnown = true;
                vehicleTypePresent = false;
            }
        });
    }

    public synchronized void resetEntities() {
        acknowledgedUpdates.clear(); awaitingProbe.clear(); pendingEntityUpdates = 0; entities.reset(); teleports.reset(); positionKnown = false;
        occupiedVehicleId = -1; vehicleTypeKnown = true; vehicleTypePresent = false;
        serverWorld = ServerWorldSnapshot.unknown(-1); serverNearbyEntities = -1;
        impulseControl("ACK_RESET_IMPULSES");
    }
    public synchronized boolean acknowledgementOverflow() { return acknowledgementOverflow; }
    public synchronized void serverNearby(int count) {
        if (count < 0) throw new IllegalArgumentException("Entity count must be nonnegative.");
        serverNearbyEntities = count;
    }
    public synchronized void serverWorld(ServerWorldSnapshot snapshot) {
        serverWorld = java.util.Objects.requireNonNull(snapshot);
        serverNearbyEntities = snapshot.nearbyEntities();
    }
    public synchronized void ability(boolean allowed) { queueAcknowledgement(() -> acknowledgedState.put("protocol-flight-allowed", allowed ? 1d : 0d)); }
    public synchronized void gameMode(int mode) {
        if (mode < 0 || mode > 3) return;
        queueAcknowledgement(() -> { acknowledgedState.put("protocol-cached-creative", mode == 1 ? 1d : 0d); acknowledgedState.put("protocol-cached-spectator", mode == 3 ? 1d : 0d); });
    }
    public synchronized void metadata(Boolean gliding, Boolean riptide) {
        metadata(gliding, riptide, null);
    }
    public synchronized void metadata(Boolean gliding, Boolean riptide, Boolean swimming) {
        if (gliding == null && riptide == null && swimming == null) return;
        queueAcknowledgement(() -> {
            if (gliding != null) {
                acknowledgedState.put("protocol-metadata-gliding", gliding ? 1d : 0d);
                acknowledgedState.compute("protocol-gliding-revision", (key, value) -> value == null ? 1d : value + 1);
            }
            if (riptide != null) acknowledgedState.put("protocol-metadata-riptide", riptide ? 1d : 0d);
            if (swimming != null) acknowledgedState.put("protocol-metadata-swimming", swimming ? 1d : 0d);
        });
    }
    public synchronized void attribute(String key, double value) {
        if (!java.util.Set.of("protocol-movement-speed", "protocol-attack-range", "protocol-jump-strength").contains(key) || !Double.isFinite(value) || value < 0) return;
        queueAcknowledgement(() -> acknowledgedState.put(key, value));
    }
    public synchronized void effect(String key, int amplifier) {
        if (!java.util.Set.of("protocol-levitation-amplifier", "protocol-jump-amplifier", "protocol-conduit-power", "protocol-speed-amplifier", "protocol-slow-falling").contains(key) || amplifier < 0) return;
        queueAcknowledgement(() -> {
            acknowledgedState.put(key, (double) amplifier);
            if (key.equals("protocol-speed-amplifier") && amplifier > 0) acknowledgedState.put("protocol-last-speed-amplifier", (double) amplifier);
        });
    }
    private void invalidateEntities() {
        acknowledgedUpdates.clear(); awaitingProbe.clear(); pendingEntityUpdates = 0; entities.invalidate(); vehicleTypeKnown = false; acknowledgedState.clear();
        impulseControl("ACK_INVALID_IMPULSES");
    }
    private void impulseControl(String type) {
        if (packets.size() == 512) { packets.removeLast(); dropped = Math.min(1_000_000, dropped + 1); }
        packets.addLast(new WirePacket(io.github.lino9999.linoac.detection.Observation.Kind.VELOCITY, type, System.nanoTime(), 0, Map.of(), java.util.Set.of()));
    }

    public record Batch(List<WirePacket> packets, int received, int dropped) {}
    public record Acknowledgement(boolean known, boolean ordered, boolean duplicate, long latencyNanos) {}
}
