package io.github.lino9999.linoac.detection;

import java.util.concurrent.TimeUnit;

public final class AutomaticPunishmentWindow {
    private long acceptedNanos;
    private boolean accepted;

    public boolean blocked(long nowNanos, long cooldownMillis) {
        if (cooldownMillis < 0 || cooldownMillis > 86_400_000) throw new IllegalArgumentException("Invalid automatic punishment cooldown.");
        return accepted && cooldownMillis > 0 && nowNanos - acceptedNanos < TimeUnit.MILLISECONDS.toNanos(cooldownMillis);
    }

    public void accepted(long nowNanos) {
        acceptedNanos = nowNanos;
        accepted = true;
    }

    public void clear() { accepted = false; }
}
