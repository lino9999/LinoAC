package io.github.lino9999.linoac.storage;

import java.util.List;

public record LogPage(int number, int totalPages, int availableRecords, List<String> lines, boolean available) {
    public LogPage {
        if (number < 1 || totalPages < 1 || availableRecords < 0) throw new IllegalArgumentException("Invalid log page metadata.");
        lines = List.copyOf(lines);
    }

    public static LogPage of(List<String> latest, int number) {
        if (number < 1) throw new IllegalArgumentException("Log page must be positive.");
        int start = (int) Math.min(latest.size(), ((long) number - 1) * 8);
        return new LogPage(number, Math.max(1, (latest.size() + 7) / 8), latest.size(),
                latest.subList(start, Math.min(latest.size(), start + 8)), true);
    }

    public static LogPage unavailable(int number) { return new LogPage(number, 1, 0, List.of(), false); }
    public boolean inRange() { return available && number <= totalPages; }
}
