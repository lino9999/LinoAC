package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import java.util.Set;

public final class PacketRules implements RuleModule {
    private static final Set<String> IDS = Set.of("badpackets-5", "badpackets-6", "badpackets-7", "badpackets-8", "badpackets-9", "badpackets-a", "badpackets-b", "badpackets-c", "badpackets-e", "badpackets-f", "badpackets-g", "badpackets-h", "badpackets-i", "badpackets-j", "badpackets-k", "badpackets-m", "badpackets-n", "badpackets-o", "badpackets-p", "badpackets-q", "badpackets-r", "badpackets-t", "badpackets-x", "badpackets-y", "badpackets-z");
    private static final Set<String> UNCOUNTED_DIG = Set.of("DROP_ITEM", "DROP_ITEM_STACK", "RELEASE_USE_ITEM", "SWAP_ITEM_WITH_OFFHAND", "CANCELLED_DIGGING");

    @Override
    public Set<String> checkIds() { return IDS; }

    @Override
    public void evaluate(Observation o, DetectionMemory m, FindingSink sink) {
        if (o.state("bukkit-event")) return;
        evaluateStateless(o, sink);
        if (o.state("source-profile-incomplete")) return;
        boolean move = o.kind() == Observation.Kind.MOVE;
        boolean arm = o.state("packet:ANIMATION") || o.state("packet:PUNCH");
        boolean attack = o.kind() == Observation.Kind.ATTACK;
        boolean interaction = attack || o.state("packet:INTERACT_ENTITY");
        boolean slot = o.state("packet:HELD_ITEM_CHANGE");
        boolean legacy = is(o, "protocol-server-at-least-1-9", 0) && is(o, "client-at-least-1-9", 0);
        boolean fast = is(o, "protocol-fast-move", 1);
        boolean spectator = o.state("spectator");
        if (o.state("packet:PLAYER_ABILITIES") && o.hasAll("timer-origin-skip", "protocol-flight-allowed", "protocol-bukkit-flight-allowed", "death-movement-age", "protocol-server-at-least-1-9")
                && is(o, "timer-origin-skip", 0) && is(o, "protocol-flight-allowed", 0) && is(o, "protocol-bukkit-flight-allowed", 0) && o.value("death-movement-age") >= 25) {
            boolean claimed = is(o, "protocol-server-at-least-1-9", 1)
                    ? o.state("claimed-flying") || o.state("claimed-allow-flight") : o.state("claimed-allow-flight");
            if (claimed) sink.sampleAboveThenDecay("badpackets-a", "Client claimed unauthorized flight abilities");
            else sink.sampleAbove("badpackets-a", false, "Client abilities did not claim flight");
        }
        if (slot && o.has("slot")) {
            double selected = o.value("slot");
            if (selected == m.get("packet.slot", -1)) sink.direct("badpackets-g", "Repeated held item slot without a transition");
            if (legacy && Boolean.FALSE.equals(ProtocolExemptions.value(o, "CREATIVE")) && !spectator && is(o, "protocol-frame-placement", 1)) sink.direct("badpackets-j", "Legacy slot switch followed placement in the same movement frame");
            m.set("packet.slot", selected);
        }
        if (o.state("packet:PLAYER_BLOCK_PLACEMENT") && is(o, "client-at-least-1-9", 0) && Boolean.FALSE.equals(ProtocolExemptions.value(o, "CREATIVE")) && o.hasAll("cursor-x", "cursor-y", "cursor-z")
                && (outsideUnit(o.value("cursor-x")) || outsideUnit(o.value("cursor-y")) || outsideUnit(o.value("cursor-z")))) sink.direct("badpackets-5", "Block interaction cursor is outside the clicked block");
        if (o.state("packet:KEEP_ALIVE") && o.hasAll("keepalive-id-high", "keepalive-id-low")) {
            double high = o.value("keepalive-id-high"), low = o.value("keepalive-id-low");
            boolean duplicate = high == m.get("packet.keepalive-high", -1) && low == m.get("packet.keepalive-low", 4294967295d);
            if (!o.state("joined") && (duplicate || high == 0 && low == 0)) sink.direct("badpackets-t", "Repeated or zero keepalive response identifier");
            m.set("packet.keepalive-high", high); m.set("packet.keepalive-low", low);
        }
        if (o.state("packet:SPECTATE") && !spectator && is(o, "protocol-cached-spectator", 0)) sink.direct("badpackets-z", "Spectate request outside spectator mode");
        if (o.state("packet:ENTITY_ACTION") && o.state("action:START_SNEAKING") && o.hasAll("packet-time-ms", "protocol-fast-move")) {
            double now = o.value("packet-time-ms"), previous = m.get("packet.sneak-time", -1);
            sink.sampleAbove("badpackets-k", now - previous < 90 && !fast, "Repeated start sneaking packet interval below 90 ms");
            m.set("packet.sneak-time", now);
        }
        if (arm) {
            m.set("packet.frame.swing", 1); m.set("packet.tick.swing", 1); m.set("packet.dig-starts", 0);
        }
        if (o.state("packet:PLAYER_DIGGING")) {
            if (o.state("dig:START_DIGGING")) sink.sampleAbove("badpackets-6", m.increment("packet.dig-starts") > 3, "More than three dig starts without an arm animation");
            boolean selected = UNCOUNTED_DIG.stream().noneMatch(name -> o.state("dig:" + name));
            if (selected && o.hasAll("client-at-least-1-21-2", "protocol-server-at-least-1-21-2")) {
                if (is(o, "client-at-least-1-21-2", 1) && is(o, "protocol-server-at-least-1-21-2", 1)) {
                    sink.sampleAbove("badpackets-8", m.increment("packet.tick.dig") > 3, "Excess block digging actions in one client tick");
                } else if (o.hasAll("packet-time-ms", "protocol-last-fast-move-ms")) {
                    double now = o.value("packet-time-ms"), elapsed = now - m.get("packet.previous-dig", -1);
                    sink.sampleAbove("badpackets-8", elapsed < 10 && now - o.value("protocol-last-fast-move-ms") >= 1000, "Consecutive block digging actions arrived within 10 ms");
                    m.set("packet.previous-dig", now);
                }
            }
        }
        if (interaction && legacy && !fast) {
            if (attack && m.get("packet.frame.swing") == 0) sink.direct("badpackets-h", "Legacy attack arrived without a preceding arm animation");
            if (!spectator && o.has("death-movement-age") && o.value("death-movement-age") >= 25 && is(o, "protocol-frame-entity-action", 1)) sink.direct("badpackets-i", "Legacy interaction followed an entity action in the same movement frame");
        }
        if (attack) { m.set("packet.frame.attack", 1); m.set("packet.tick.attack", 1); }
        post(o, m, sink, "m", "PLAYER_BLOCK_PLACEMENT", move);
        post(o, m, sink, "n", "PLAYER_DIGGING", move);
        post(o, m, sink, "p", "ENTITY_ACTION", move);
        post(o, m, sink, "r", "HELD_ITEM_CHANGE", move);
        post(o, m, sink, "x", arm ? "" : "ANIMATION", move);
        if (o.state("packet:CLIENT_TICK_END") && is(o, "client-at-least-1-21-2", 1)) {
            if (!spectator && m.get("packet.tick.attack") > 0 && m.get("packet.tick.swing") == 0) sink.direct("badpackets-9", "Client tick ended with an attack and no arm animation");
            m.set("packet.tick.attack", 0); m.set("packet.tick.swing", 0); m.set("packet.tick.dig", 0);
        }
        if (move) {
            if (is(o, "client-at-least-1-21-2", 0) && is(o, "client-at-least-1-9", 1) && !spectator
                    && m.get("packet.frame.attack") > 0 && m.get("packet.frame.swing") == 0 && m.get("packet.previous.swing") == 0) sink.direct("badpackets-7", "Attack frame and previous frame contained no arm animation");
            if (o.state("position-packet") || o.state("vehicle")) m.set("packet.no-position", 0);
            else if (legacy && m.increment("packet.no-position") > 20) sink.direct("badpackets-b", "More than twenty legacy movement packets omitted position");
            m.set("packet.previous.swing", m.get("packet.frame.swing")); m.set("packet.frame.swing", 0); m.set("packet.frame.attack", 0);
        }
        if (o.state("packet:STEER_VEHICLE")) m.set("packet.no-position", 0);
    }

    public void evaluateStateless(Observation o, FindingSink sink) {
        if (o.state("bukkit-event")) return;
        boolean rotation = o.kind() == Observation.Kind.ROTATION || o.state("nonfinite-rotation")
                || o.state("source-profile-incomplete") && o.state("rotation-packet");
        if (rotation && (o.has("pitch") && (!Double.isFinite(o.value("pitch")) || Math.abs(o.value("pitch")) > 90)
                || o.has("yaw") && !Double.isFinite(o.value("yaw"))))
            sink.direct("badpackets-c", "Rotation is outside the protocol range");
        if ((o.kind() == Observation.Kind.ATTACK || o.state("packet:INTERACT_ENTITY")) && !o.state("spectator")
                && o.hasAll("target-id", "self-id") && o.value("target-id") == o.value("self-id"))
            sink.direct("badpackets-e", "Player interacted with its own entity ID");
        if (o.state("packet:STEER_VEHICLE") && o.hasAll("steer-forward", "steer-sideways")
                && (!Double.isFinite(o.value("steer-forward")) || !Double.isFinite(o.value("steer-sideways"))
                || Math.abs(o.value("steer-forward")) > (double) .98f || Math.abs(o.value("steer-sideways")) > (double) .98f))
            sink.direct("badpackets-f", "Vehicle input exceeds the client input range");
        if (o.state("packet:HELD_ITEM_CHANGE") && o.has("slot")) {
            if (o.value("slot") < 0) sink.direct("badpackets-o", "Negative held item slot");
            if (o.value("slot") > 8) sink.direct("badpackets-q", "Held item slot exceeds the hotbar range");
        }
        if (o.kind() == Observation.Kind.MOVE && o.state("position-packet") && (o.hasAll("x", "y", "z") || o.state("nonfinite-position"))) {
            boolean invalid = o.state("nonfinite-position");
            for (String axis : new String[]{"x", "y", "z"}) if (o.has(axis)) invalid |= !Double.isFinite(o.value(axis)) || Math.abs(o.value(axis)) >= Integer.MAX_VALUE;
            if (invalid) sink.direct("badpackets-y", "Position cannot be represented safely by the server");
        }
    }

    private static void post(Observation o, DetectionMemory m, FindingSink sink, String id, String packet, boolean move) {
        if (!o.has("packet-time-ms")) return;
        String key = "packet.post." + id;
        double now = o.value("packet-time-ms"), previous = m.get(key + ".time");
        if (move) {
            if (id.equals("p") && !is(o, "protocol-server-at-least-1-9", 0) || id.equals("x") && !(is(o, "protocol-server-at-least-1-9", 0) && is(o, "client-at-least-1-9", 0))) return;
            var modes = ProtocolExemptions.evaluate(o, "CREATIVE", "SPECTATOR");
            if (id.equals("p") && (!modes.known() || modes.active())) return;
            if ((id.equals("m") || id.equals("r")) && !modes.known()) { m.set(key + ".time", now); return; }
            boolean exempt = (id.equals("m") || id.equals("r")) && modes.active();
            if (id.equals("m")) {
                if (!o.hasAll("protocol-bucket-age", "protocol-held-bucket", "timer-web-exempt")) { m.set(key + ".time", now); return; }
                var context = ProtocolExemptions.evaluate(o, "WEB", "LIQUID", "EMPTIED_BUCKET");
                exempt |= o.value("protocol-bucket-age") < 50 || is(o, "protocol-held-bucket", 1) || context.active();
                if (!exempt && !context.known()) { m.set(key + ".time", now); return; }
            }
            if (m.get(key) > 0 && !exempt) {
                sink.sampleAbove("badpackets-" + id, now - previous > 40 && now - previous < 100, "Action followed the previous movement packet within 5 ms");
                m.set(key, 0);
            }
            m.set(key + ".time", now);
        } else if (packet.isEmpty() || o.state("packet:" + packet)) {
            if (now - previous < 5) m.set(key, 1);
        }
    }

    private static boolean is(Observation o, String key, double value) { return o.has(key) && o.value(key) == value; }
    private static boolean outsideUnit(double value) { return !Double.isFinite(value) || value < 0 || value > 1; }
}
