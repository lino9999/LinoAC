package io.github.lino9999.linoac.detection.collector;

public final class ContactClock {
    private final int recovery;
    private int value, negativeRun;
    private boolean known = true, exact = true;

    public ContactClock(int initial, int recovery) {
        if (initial < 0 || recovery < 1) throw new IllegalArgumentException("Invalid contact clock bounds.");
        value = initial; this.recovery = recovery;
    }

    public void update(Boolean contact) {
        if (contact == null) { known = false; exact = false; negativeRun = 0; }
        else if (contact) { value = 0; known = true; exact = true; negativeRun = 0; }
        else if (known) value = increment(value);
        else if (++negativeRun >= recovery) { value = recovery; known = true; }
    }

    public Integer value() { return known ? value : null; }
    public boolean exact() { return known && exact; }
    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
}
