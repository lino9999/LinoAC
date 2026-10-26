package io.github.lino9999.linoac.detection.collector;

public final class ContactAgeEvidence {
    private final int maximum;
    private int minimumAge, maximumAge;

    public ContactAgeEvidence(int initialAge, int maximum) {
        if (initialAge < 0 || maximum < 1) throw new IllegalArgumentException("Contact age bounds must be nonnegative.");
        this.maximum = maximum; minimumAge = maximumAge = Math.min(initialAge, maximum);
    }

    public void position(Boolean contact) {
        if (Boolean.TRUE.equals(contact)) minimumAge = maximumAge = 0;
        else if (Boolean.FALSE.equals(contact)) {
            minimumAge = Math.min(maximum, minimumAge + 1); maximumAge = Math.min(maximum, maximumAge + 1);
        } else { minimumAge = 0; maximumAge = Math.min(maximum, maximumAge + 1); }
    }

    public Integer age() { return minimumAge == maximumAge ? minimumAge : null; }
    public int minimumAge() { return minimumAge; }
    public int maximumAge() { return maximumAge; }
}
