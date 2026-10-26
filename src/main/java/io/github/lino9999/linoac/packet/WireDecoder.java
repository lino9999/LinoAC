package io.github.lino9999.linoac.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import io.github.lino9999.linoac.detection.Observation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class WireDecoder {
    public WirePacket decode(PacketReceiveEvent event, PacketBuffer buffer) {
        String type = event.getPacketType().toString();
        long now = System.nanoTime(), token = 0;
        Observation.Kind kind = Observation.Kind.PACKET;
        Map<String, Double> values = new HashMap<>();
        values.put("packet-time-ms", (double) System.currentTimeMillis());
        Set<String> states = new HashSet<>();
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            kind = Observation.Kind.MOVE;
            var packet = new WrapperPlayClientPlayerFlying(event);
            var location = packet.getLocation();
            if (packet.hasPositionChanged()) {
                states.add("position-changed");
                states.add("position-packet");
                values.put("x", location.getX()); values.put("y", location.getY()); values.put("z", location.getZ());
                if (!Double.isFinite(location.getX()) || !Double.isFinite(location.getY()) || !Double.isFinite(location.getZ())) states.add("nonfinite-position");
                if (Math.abs(location.getX()) > 30_000_001 || Math.abs(location.getZ()) > 30_000_001 || Math.abs(location.getY()) > 20_000_000) states.add("outside-world");
            }
            if (packet.hasRotationChanged()) {
                states.add("rotation-changed");
                states.add("rotation-packet");
                values.put("yaw", (double) location.getYaw()); values.put("pitch", (double) location.getPitch());
                if (!Float.isFinite(location.getYaw()) || !Float.isFinite(location.getPitch())) states.add("nonfinite-rotation");
                if (Math.abs(location.getPitch()) > 90) states.add("invalid-pitch");
            }
            if (packet.isOnGround()) states.add("client-ground");
            if (packet.isHorizontalCollision()) states.add("client-horizontal-collision");
            if (states.contains("nonfinite-position") || states.contains("outside-world") || states.contains("nonfinite-rotation") || Math.abs(location.getPitch()) > 90) event.setCancelled(true);
        } else switch (type) {
            case "ANIMATION", "PUNCH" -> {
                kind = Observation.Kind.SWING;
                values.put("click-time-ms", (double) System.currentTimeMillis());
            }
            case "INTERACT_ENTITY" -> {
                var packet = new WrapperPlayClientInteractEntity(event);
                values.put("target-id", (double) packet.getEntityId());
                states.add("interact:" + packet.getAction().name());
                if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) kind = Observation.Kind.ATTACK;
                else { kind = Observation.Kind.USE; states.add("entity-interaction"); }
            }
            case "ATTACK" -> {
                kind = Observation.Kind.ATTACK;
                values.put("target-id", (double) new WrapperPlayClientAttack(event).getEntityId());
            }
            case "PLAYER_ABILITIES" -> {
                var packet = new WrapperPlayClientPlayerAbilities(event);
                if (packet.isFlying()) states.add("claimed-flying");
                if (packet.isFlightAllowed().orElse(false)) states.add("claimed-allow-flight");
                if (packet.isInCreativeMode().orElse(false)) states.add("claimed-creative");
            }
            case "HELD_ITEM_CHANGE" -> {
                int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
                values.put("slot", (double) slot);
                if (slot < 0 || slot > 8) event.setCancelled(true);
            }
            case "STEER_VEHICLE" -> {
                var packet = new WrapperPlayClientSteerVehicle(event);
                values.put("steer-forward", (double) packet.getForward()); values.put("steer-sideways", (double) packet.getSideways());
                if (packet.isJump()) states.add("vehicle-jump");
            }
            case "VEHICLE_MOVE" -> {
                var packet = new WrapperPlayClientVehicleMove(event);
                var position = packet.getPosition();
                values.put("vehicle-x", position.getX()); values.put("vehicle-y", position.getY()); values.put("vehicle-z", position.getZ());
                if (!Double.isFinite(position.getX()) || !Double.isFinite(position.getY()) || !Double.isFinite(position.getZ())) {
                    states.add("nonfinite-position"); event.setCancelled(true);
                }
            }
            case "PLAYER_INPUT" -> {
                var packet = new WrapperPlayClientPlayerInput(event);
                values.put("steer-forward", (packet.isForward() ? 1d : 0d) - (packet.isBackward() ? 1d : 0d));
                values.put("steer-sideways", (packet.isLeft() ? 1d : 0d) - (packet.isRight() ? 1d : 0d));
                if (packet.isJump()) states.add("input-jump");
                if (packet.isShift()) states.add("input-sneak");
                if (packet.isSprint()) states.add("input-sprint");
            }
            case "ENTITY_ACTION" -> {
                var packet = new WrapperPlayClientEntityAction(event);
                values.put("action-entity-id", (double) packet.getEntityId());
                states.add("action:" + packet.getAction().name());
            }
            case "CLIENT_STATUS" -> {
                var packet = new WrapperPlayClientClientStatus(event);
                states.add("client-status:" + packet.getAction().name());
            }
            case "PLAYER_BLOCK_PLACEMENT" -> {
                kind = Observation.Kind.PLACE;
                var packet = new WrapperPlayClientPlayerBlockPlacement(event);
                var block = packet.getBlockPosition(); var cursor = packet.getCursorPosition();
                values.put("block-x", (double) block.getX()); values.put("block-y", (double) block.getY()); values.put("block-z", (double) block.getZ());
                values.put("cursor-x", (double) cursor.getX()); values.put("cursor-y", (double) cursor.getY()); values.put("cursor-z", (double) cursor.getZ());
                values.put("block-face", (double) packet.getFaceId()); values.put("sequence", (double) packet.getSequence());
            }
            case "PLAYER_DIGGING" -> {
                kind = Observation.Kind.DIG;
                var packet = new WrapperPlayClientPlayerDigging(event); var block = packet.getBlockPosition();
                states.add("dig:" + packet.getAction().name());
                values.put("block-x", (double) block.getX()); values.put("block-y", (double) block.getY()); values.put("block-z", (double) block.getZ());
                values.put("block-face", (double) packet.getBlockFaceId()); values.put("sequence", (double) packet.getSequence());
                if (packet.getAction().name().equals("RELEASE_USE_ITEM")) kind = Observation.Kind.RELEASE;
            }
            case "USE_ITEM" -> {
                kind = Observation.Kind.USE;
                var packet = new WrapperPlayClientUseItem(event);
                values.put("sequence", (double) packet.getSequence());
            }
            case "CLICK_WINDOW" -> {
                kind = Observation.Kind.INVENTORY;
                var packet = new WrapperPlayClientClickWindow(event);
                values.put("window-id", (double) packet.getWindowId()); values.put("inventory-slot", (double) packet.getSlot());
                values.put("inventory-button", (double) packet.getButton());
            }
            case "KEEP_ALIVE", "PONG" -> {
                kind = Observation.Kind.ACKNOWLEDGEMENT;
                boolean ping = type.equals("PONG");
                token = ping ? new WrapperPlayClientPong(event).getId() : new WrapperPlayClientKeepAlive(event).getId();
                if (!ping) {
                    values.put("keepalive-id-high", (double) (token >> 32));
                    values.put("keepalive-id-low", (double) (token & 0xffffffffL));
                }
                var acknowledgement = buffer.acknowledge(token, now, ping);
                if (acknowledgement.known()) states.add("ack-known");
                if (acknowledgement.ordered()) states.add("ack-ordered");
                if (!ping && acknowledgement.known()) states.add("keepalive-known");
                if (!ping && acknowledgement.duplicate()) states.add("keepalive-duplicate");
                if (!ping && token == 0) states.add("keepalive-zero");
                values.put("ack-latency-ms", acknowledgement.latencyNanos() / 1_000_000d);
                if (acknowledgement.known()) values.put("ack-sent-nanos", (double) (now - acknowledgement.latencyNanos()));
            }
            default -> { }
        }
        states.add("packet:" + type);
        return new WirePacket(kind, type, now, token, values, states);
    }
}
