package io.github.lino9999.linoac.detection.combat;

import io.github.lino9999.linoac.detection.Observation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SequenceContext {
    private long lastMovementMillis;
    private long lastRangeAttackMillis;
    private long lastArrowMillis;
    private boolean fast;
    private boolean postAttack;
    private boolean placed;
    private boolean dug;
    private boolean released;
    private boolean attacked;
    private boolean interacted;
    private boolean reachA;
    private boolean reachB;
    private boolean hitboxA;
    private String angularTarget;
    private int previousTarget;
    private int targetChanges;
    private int flyingSinceAttack;
    private int previousFlyingSinceAttack;
    private int frequencySamples;
    private int equalFrequencySamples;
    private final long[] attackIntervals = new long[10];
    private int attackIntervalCount;

    public Observation update(Observation input) {
        Map<String, Double> values = new HashMap<>(input.values());
        Set<String> states = new HashSet<>(input.states());
        values.keySet().removeIf(key -> key.startsWith("sequence-"));
        states.removeIf(key -> key.startsWith("sequence-"));
        states.add("sequence-context-known");
        if (input.state("nonfinite-position") || input.state("nonfinite-rotation") || input.state("outside-world") || input.state("invalid-pitch")) {
            if (!input.state("bukkit-event") && input.kind() == Observation.Kind.MOVE) angularTarget = null;
            return observation(input, values, states);
        }
        if (input.state("source-arrow-launch")) {
            arrow(input, values);
        } else if (!input.state("bukkit-event")) {
            if (input.kind() == Observation.Kind.MOVE && input.has("packet-time-ms")) {
                movement(input, values, states);
            } else if (input.kind() == Observation.Kind.ATTACK && input.has("packet-time-ms")) {
                attack(input, values);
                states.add("sequence-entity-event");
            } else if (input.state("entity-interaction") || input.state("interact:INTERACT") || input.state("interact:INTERACT_AT")) {
                interacted = true;
                reachB = false;
                states.add("sequence-entity-event");
            } else if (input.state("packet:PLAYER_BLOCK_PLACEMENT")) {
                placed = true;
                if (attacked && !interacted) states.add("sequence-autoblock-d-ready");
            } else if (input.state("packet:PLAYER_DIGGING")) {
                dug = true;
                if (input.state("dig:RELEASE_USE_ITEM")) released = true;
            }
        }
        if (fast) states.add("sequence-fast");
        return observation(input, values, states);
    }

    private void movement(Observation input, Map<String, Double> values, Set<String> states) {
        if (angularTarget != null && angularTarget.equals(AngularTarget.identity(input))) states.add("sequence-hitbox-b-ready");
        angularTarget = null;
        long now = (long) input.value("packet-time-ms");
        long elapsed = now - lastMovementMillis;
        fast = elapsed <= 5;
        if (postAttack) values.put("sequence-killaura-a-ms", (double) elapsed);
        postAttack = false;
        lastMovementMillis = now;
        flyingSinceAttack = increment(flyingSinceAttack);
        if (reachA) {
            states.add("sequence-reach-a-ready");
            if (input.state("source-reach-a-known") && !input.state("source-reach-a-early-return")) reachA = false;
        }
        if (reachB) {
            states.add("sequence-reach-b-ready");
            if (input.state("source-reach-b-known") && !input.state("source-reach-b-early-return")) reachB = false;
        }
        if (hitboxA) {
            states.add("sequence-hitbox-a-ready");
            if (input.state("source-hitbox-a-known") && !input.state("source-hitbox-a-early-return")) hitboxA = false;
        }
        placed = false;
        dug = false;
        released = false;
        attacked = false;
        interacted = false;
        targetChanges = 0;
    }

    private void attack(Observation input, Map<String, Double> values) {
        angularTarget = input.state("target-player") && input.has("target-id") ? AngularTarget.identity(input) : null;
        long now = (long) input.value("packet-time-ms");
        values.put("sequence-autoblock-a", placed ? 1d : 0d);
        values.put("sequence-autoblock-b", released ? 1d : 0d);
        values.put("sequence-autoblock-c", dug ? 1d : 0d);
        attacked = true;
        if (now - lastMovementMillis < 5) postAttack = true;
        if (input.has("target-id")) {
            int target = (int) input.value("target-id");
            if (target != previousTarget && input.state("legacy-client")
                    && input.has("source-server-at-least-1-9") && input.value("source-server-at-least-1-9") == 0) {
                targetChanges = increment(targetChanges);
                if (targetChanges > 1) values.put("sequence-killaura-d-target-changes", (double) targetChanges);
            }
            previousTarget = target;
        }
        if (input.state("target-player")) {
            reachA = true;
            reachB = true;
            hitboxA = true;
        } else {
            reachB = false;
        }
        if (input.has("eligible-swing-cps") && input.value("eligible-swing-cps") > 7.2
                && flyingSinceAttack < 4 && previousFlyingSinceAttack < 4) {
            frequencySamples++;
            if (flyingSinceAttack == previousFlyingSinceAttack) equalFrequencySamples++;
            if (frequencySamples == 30) {
                values.put("sequence-killaura-j-equal-count", (double) equalFrequencySamples);
                frequencySamples = 0;
                equalFrequencySamples = 0;
            }
        }
        previousFlyingSinceAttack = flyingSinceAttack;
        flyingSinceAttack = 0;
        if (!fast) {
            attackIntervals[attackIntervalCount++] = now - lastRangeAttackMillis;
            if (attackIntervalCount == attackIntervals.length) {
                long minimum = Long.MAX_VALUE;
                long maximum = Long.MIN_VALUE;
                for (long interval : attackIntervals) { minimum = Math.min(minimum, interval); maximum = Math.max(maximum, interval); }
                values.put("sequence-killaura-k-range-ms", (double) (maximum - minimum));
                attackIntervalCount = 0;
            }
            lastRangeAttackMillis = now;
        }
    }

    private void arrow(Observation input, Map<String, Double> values) {
        if (!input.hasAll("event-time-ms", "projectile-speed") || input.value("projectile-speed") <= 2
                || !input.state("bow-held") || input.state("crossbow-held") || input.state("multishot-held")) return;
        long now = (long) input.value("event-time-ms");
        values.put("sequence-fastbow-interval-ms", (double) (now - lastArrowMillis));
        lastArrowMillis = now;
    }

    private int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }

    private Observation observation(Observation input, Map<String, Double> values, Set<String> states) {
        return new Observation(input.kind(), input.tick(), input.nanos(), values, states);
    }
}
