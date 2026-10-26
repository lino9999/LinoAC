package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.player.ProtocolExemptions;

import java.util.UUID;

public final class AngularTarget {
    private static final String PREFIX = "source-angular-target:";

    private AngularTarget() {}

    public static Measurement measure(double attackerX, double attackerZ, float rawYaw, double targetX, double targetZ) {
        if (!Double.isFinite(attackerX) || !Double.isFinite(attackerZ) || !Float.isFinite(rawYaw)
                || !Double.isFinite(targetX) || !Double.isFinite(targetZ)) return new Measurement(false, Double.NaN);
        double dx = targetX - attackerX, dz = targetZ - attackerZ;
        double radians = rawYaw * 3.1415927f / 180f;
        double lookX = -Math.sin(radians) * .5, lookZ = Math.cos(radians) * .5;
        double denominator = Math.sqrt(dx * dx + dz * dz) * Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (!(denominator > 0) || !Double.isFinite(denominator)) return new Measurement(false, Double.NaN);
        double cosine = Math.max(-1, Math.min(1, (dx * lookX + dz * lookZ) / denominator));
        double degrees = Math.min(180, Math.toDegrees((float) Math.acos(cosine)));
        return new Measurement(Double.isFinite(degrees), degrees);
    }

    public static String token(UUID target) { return PREFIX + target; }

    public static String identity(Observation input) {
        String identity = null;
        for (String state : input.states()) if (state.startsWith(PREFIX)) {
            if (identity != null || state.length() == PREFIX.length()) return null;
            identity = state;
        }
        return identity;
    }

    public static ProtocolExemptions.Result exemptions(Observation input, Boolean targetVehicle, Boolean targetBoat) {
        var attacker = ProtocolExemptions.evaluate(input, "CREATIVE", "VEHICLE", "DEAD");
        boolean active = attacker.active() || Boolean.TRUE.equals(targetVehicle) || Boolean.TRUE.equals(targetBoat);
        return new ProtocolExemptions.Result(active || attacker.known() && targetVehicle != null && targetBoat != null, active);
    }

    public record Measurement(boolean known, double angleDegrees) {}
}
