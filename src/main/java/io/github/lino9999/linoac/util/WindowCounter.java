package io.github.lino9999.linoac.util;

public final class WindowCounter {
    private long start;
    private int count;

    public int add(long now) {
        if (now - start >= 1_000_000_000L) {
            start = now;
            count = 0;
        }
        return ++count;
    }

    public int count(long now) { return now - start >= 1_000_000_000L ? 0 : count; }
}
