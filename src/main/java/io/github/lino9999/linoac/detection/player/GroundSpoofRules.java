package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import java.util.Set;

public final class GroundSpoofRules implements RuleModule {
    private static final Set<String> IDS = Set.of("groundspoof-a", "groundspoof-b", "groundspoof-c");
    private static final String[] A = {"JOINED", "STAIRS", "CHUNK", "SHULKER", "RIPTIDE", "LIQUID", "TELEPORT", "VOID", "WORLD_CHANGE", "SCAFFOLDING", "SWIMMING", "BOAT", "GLIDING", "WATERLOGGED", "HONEY", "SLOW_FALLING", "BLOCK_PLACE", "SNOW", "WEB", "FLIGHT", "SLIME", "PLACED_SLIME", "HAPPY_GHAST", "SHULKER_BOX", "NEAR_GROUND", "CANCELLED_PLACE", "LEVITATION"};
    private static final String[] B = {"FLIGHT", "CREATIVE", "BOAT", "TELEPORT", "SHULKER", "SHULKER_BOX", "CHUNK", "CARPET", "VEHICLE", "SCAFFOLDING", "DEAD", "HAPPY_GHAST", "GLIDING"};
    private static final String[] C = {"TELEPORT", "ANVIL", "LIQUID", "WATERLOGGED", "RIPTIDE", "ELYTRA", "GLIDING", "JOINED", "CLIMBABLE", "LILY_PAD", "SNOW", "WORLD_CHANGE", "VELOCITY", "WEB", "NEAR_GROUND", "SLOW_FALLING", "LEVITATION", "SHULKER", "SHULKER_BOX", "HONEY", "SLIME", "FLIGHT", "VOID", "CANCELLED_PLACE", "PLACED_SLIME", "BLOCK_PLACE"};

    @Override
    public Set<String> checkIds() { return IDS; }

    @Override
    public void evaluate(Observation o, DetectionMemory memory, FindingSink sink) {
        if (o.kind() != Observation.Kind.MOVE || o.state("bukkit-event")) return;
        boolean position = o.state("position-packet");
        if (position && o.hasAll("y", "protocol-air-volume27")) {
            double y = o.value("y");
            boolean allowedFraction = y % 1 == .6 || y % .1 == .5625;
            var exemptions = ProtocolExemptions.evaluate(o, B);
            boolean suspicious = o.state("client-ground") && y % .015625 != 0 && o.value("protocol-air-volume27") == 1 && !allowedFraction && !exemptions.active();
            if (exemptions.known()) sink.sampleAbove("groundspoof-b", suspicious, "Ground claim contradicted the retained empty volume and vertical grid");
        }
        if (!o.hasAll("timer-origin-skip", "timer-signed-position-dy", "teleport-movement-age") || o.value("timer-origin-skip") == 1) return;
        double dy = o.value("timer-signed-position-dy"), teleportAge = o.value("teleport-movement-age");
        if (!o.state("server-teleport-match") && o.has("protocol-air-volume27") && Math.abs(dy) - .3116 > .005) {
            var exemptions = ProtocolExemptions.evaluate(o, A);
            boolean suspicious = o.state("client-ground") && o.value("protocol-air-volume27") == 1 && dy < (double) -.2f && teleportAge >= 20 && !exemptions.active();
            if (exemptions.known()) sink.sampleAbove("groundspoof-a", suspicious, "Client claimed ground during downward movement through an empty volume");
        }
        if (position && o.has("protocol-air-run")) {
            var exemptions = ProtocolExemptions.evaluate(o, C);
            boolean suspicious = dy < -.5 && o.value("protocol-air-run") < 5 && teleportAge >= 40 && !exemptions.active();
            if (exemptions.known()) sink.sampleAbove("groundspoof-c", suspicious, "Recent ground claim contradicted downward movement");
        }
    }
}
