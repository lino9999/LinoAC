package io.github.lino9999.linoac.detection;

public final class FlagEligibility {
    private FlagEligibility() {}

    public static boolean allowed(Context context, int maximumPing, double minimumTps) {
        return context.online() && !context.frozen() && !context.bypass() && !context.ignoredBedrock() && !context.excludedWorld()
                && context.ping() <= maximumPing && Double.isFinite(context.tps()) && context.tps() >= minimumTps;
    }

    public record Context(boolean online, boolean frozen, boolean bypass, boolean ignoredBedrock, boolean excludedWorld, int ping, double tps) {}
}
