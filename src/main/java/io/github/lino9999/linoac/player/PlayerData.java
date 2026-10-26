package io.github.lino9999.linoac.player;

import io.github.lino9999.linoac.check.CheckType;
import io.github.lino9999.linoac.util.WindowCounter;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerData {
    public final UUID id;
    public final long joinedMillis = System.currentTimeMillis();
    public final io.github.lino9999.linoac.detection.ViolationHistory historyTotals = new io.github.lino9999.linoac.detection.ViolationHistory();
    public final io.github.lino9999.linoac.detection.AutomaticPunishmentWindow automaticPunishment = new io.github.lino9999.linoac.detection.AutomaticPunishmentWindow();
    public final Map<CheckType, Double> buffers = new EnumMap<>(CheckType.class);
    public final Map<CheckType, Integer> violations = new EnumMap<>(CheckType.class);
    public final Map<CheckType, Long> lastFlags = new EnumMap<>(CheckType.class);
    public final Map<CheckType, Long> lastAlerts = new EnumMap<>(CheckType.class);
    public final Map<CheckType, Long> lastPunishments = new EnumMap<>(CheckType.class);
    public final WindowCounter placements = new WindowCounter();
    public final WindowCounter clicks = new WindowCounter();
    public final ArrayDeque<BoundingBox> history = new ArrayDeque<>();
    public final Set<Integer> attacked = new HashSet<>();
    public Location previous;
    public Location safe;
    public long graceUntil;
    public long movementGraceUntil;
    public long lastCombat;
    public long lastMoveTick;
    public long environmentUntil;
    public double previousDy;
    public boolean previousGround;
    public int airTicks;
    public int liquidTicks;
    public int swings;
    public double clicksPerSecond;
    public boolean frozen;
    public boolean alerts;
    public boolean verbose;
    public boolean bedrock;
    public String brand = "unknown";
    public String clientVersion = "unknown";

    public PlayerData(UUID id, long graceUntil) {
        this.id = id;
        this.graceUntil = graceUntil;
    }

    public int totalViolations() { return violations.values().stream().mapToInt(Integer::intValue).sum(); }

    public void reset() {
        automaticPunishment.clear();
        historyTotals.clear();
        buffers.clear();
        violations.clear();
        lastFlags.clear();
        lastAlerts.clear();
        lastPunishments.clear();
    }
}
