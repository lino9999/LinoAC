package io.github.lino9999.linoac.detection.player;

import io.github.lino9999.linoac.detection.DetectionMemory;
import io.github.lino9999.linoac.detection.Observation;
import io.github.lino9999.linoac.detection.RuleModule;
import io.github.lino9999.linoac.detection.Statistics;

import java.util.Set;

public final class TimerRules implements RuleModule {
    private static final Set<String> IDS = Set.of("timer-a", "timer-d");
    private static final String A_PREVIOUS = "timer:a:previous-millis";
    private static final String A_COUNT = "timer:a:sample-count";
    private static final String A_MEAN = "timer:a:mean-millis";
    private static final String D_PREVIOUS = "timer:d:previous-millis";
    private static final String D_BALANCE = "timer:d:balance-millis";
    private static final String D_COUNTER = "timer:d:movement-count";

    @Override
    public Set<String> checkIds() { return IDS; }

    @Override
    public void evaluate(Observation observation, DetectionMemory memory, FindingSink sink) {
        if (observation.state("bukkit-event") || observation.kind() == Observation.Kind.ROTATION) return;
        if (observation.kind() == Observation.Kind.SERVER_POSITION && observation.state("packet:SERVER_POSITION_AND_LOOK")) {
            append(memory, 150);
            if (knownFlag(observation, "server-at-least-1-9")) {
                memory.increment(D_BALANCE, observation.value("server-at-least-1-9") == 1 ? -150 : -50);
            }
            return;
        }
        boolean primary = observation.kind() == Observation.Kind.MOVE
                || observation.kind() == Observation.Kind.PACKET && observation.state("packet:VEHICLE_MOVE");
        if (!primary) {
            if (knownFlag(observation, "client-at-least-1-17") && observation.value("client-at-least-1-17") == 1
                    && (observation.state("packet:PLAYER_BLOCK_PLACEMENT") || observation.state("packet:USE_ITEM"))) {
                append(memory, 50);
            }
            return;
        }
        if (!knownFlag(observation, "timer-origin-skip") || observation.value("timer-origin-skip") == 1
                || !observation.has("packet-time-ms")) return;
        double now = observation.value("packet-time-ms");
        if (!Double.isFinite(now) || now < 0 || now != Math.rint(now)) return;
        boolean timingKnown = finiteNonnegative(observation, "timer-tps") && knownFlag(observation, "timer-vehicle-exempt");
        boolean exempt = !timingKnown || observation.state("joined") || observation.value("timer-tps") < 19.25
                || observation.value("timer-vehicle-exempt") == 1;
        average(observation, memory, sink, now, timingKnown, exempt);
        balance(observation, memory, sink, now, timingKnown, exempt);
    }

    private void average(Observation observation, DetectionMemory memory, FindingSink sink, double now, boolean timingKnown, boolean exempt) {
        double elapsed = now - memory.get(A_PREVIOUS);
        if (!exempt && elapsed > 4) append(memory, elapsed);
        if (timingKnown && memory.get(A_COUNT) == 50 && knownFlag(observation, "timer-web-exempt")
                && finitePositive(observation, "timer-speed-limit")) {
            double mean = memory.get(A_MEAN);
            double rate = 50 / mean;
            double maximum = observation.value("timer-speed-limit") + (observation.value("timer-web-exempt") == 1 ? .05 : 0);
            sink.sampleAbove("timer-a", rate >= maximum, "speed=" + rate * 100 + "% delay=" + (long) elapsed);
        }
        memory.set(A_PREVIOUS, now);
    }

    private void balance(Observation observation, DetectionMemory memory, FindingSink sink, double now, boolean timingKnown, boolean exempt) {
        double previous = memory.get(D_PREVIOUS);
        double counter = memory.get(D_COUNTER);
        boolean known = timingKnown && knownFlag(observation, "client-at-least-1-9")
                && knownFlag(observation, "timer-vehicle-type-present") && finiteNonnegative(observation, "timer-balance-limit");
        if (previous != 0 && !exempt && known) {
            double elapsed = now - previous;
            double amount = memory.increment(D_BALANCE, 50 - elapsed);
            if (amount > observation.value("timer-balance-limit") && observation.value("timer-vehicle-type-present") == 0) {
                sink.sampleAboveThenDecay("timer-d", "balance=" + (long) amount + " delay=" + (long) elapsed);
                memory.set(D_BALANCE, -200);
            }
            int interval = observation.value("client-at-least-1-9") == 1 ? 400 : 3000;
            if (counter % interval == 0) {
                memory.set(D_BALANCE, -200);
                counter = 0;
            }
        }
        memory.set(D_COUNTER, counter + 1);
        memory.set(D_PREVIOUS, now);
    }

    private void append(DetectionMemory memory, double elapsed) {
        double[] samples = memory.window("timer:a:intervals", elapsed, 50);
        memory.set(A_COUNT, samples.length);
        memory.set(A_MEAN, Statistics.mean(samples));
    }

    private boolean knownFlag(Observation observation, String key) {
        return observation.has(key) && (observation.value(key) == 0 || observation.value(key) == 1);
    }

    private boolean finiteNonnegative(Observation observation, String key) {
        return observation.has(key) && Double.isFinite(observation.value(key)) && observation.value(key) >= 0;
    }

    private boolean finitePositive(Observation observation, String key) {
        return finiteNonnegative(observation, key) && observation.value(key) > 0;
    }
}
