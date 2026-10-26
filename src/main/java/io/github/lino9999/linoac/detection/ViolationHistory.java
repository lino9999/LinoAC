package io.github.lino9999.linoac.detection;

public final class ViolationHistory {
    private int total;
    private int combat;
    private int movement;
    private int player;
    private int autoClicker;
    private int scaffold;
    private int timer;

    public void recordAccepted(String category, String family, boolean experimental) {
        if (category == null || category.isBlank() || family == null || family.isBlank()) throw new IllegalArgumentException("Violation category and family are required.");
        if (experimental || family.equals("improbable")) return;
        if (family.equals("timer")) {
            timer = increment(timer);
            return;
        }
        total = increment(total);
        if (family.equals("autoclicker")) {
            autoClicker = increment(autoClicker);
            return;
        }
        if (family.equals("scaffold")) {
            scaffold = increment(scaffold);
            player = increment(player);
            return;
        }
        switch (category) {
            case "combat" -> combat = increment(combat);
            case "movement" -> movement = increment(movement);
            case "player" -> player = increment(player);
            default -> {}
        }
    }

    public void clear() {
        total = combat = movement = player = autoClicker = scaffold = timer = 0;
    }

    public Snapshot snapshot() {
        return new Snapshot(total, combat, movement, player, autoClicker, scaffold, timer);
    }

    private static int increment(int current) {
        return current == Integer.MAX_VALUE ? current : current + 1;
    }

    public record Snapshot(int total, int combat, int movement, int player, int autoClicker, int scaffold, int timer) {}
}
