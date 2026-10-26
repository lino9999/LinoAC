package io.github.lino9999.linoac.detection.collector;

import io.github.lino9999.linoac.packet.ServerWorldSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MountedInputState {
    private Object mount;
    private double x, y, z, dy, previousDy, horizontal;
    private int packetCount, airAge;
    private int knownAirRun;
    private List<ServerWorldSnapshot.Cell> materials;
    private Integer nearbyBoats;
    private Boolean otherLiving, happyGhast, allAir;
    private Integer frameBoats;
    private Boolean frameLiving, frameGhast;
    private final Map<String, ContactClock> ages = new HashMap<>();

    public MountedInputState() {
        ages.put("ice", new ContactClock(500, 100)); ages.put("liquid", new ContactClock(150, 100)); ages.put("slime", new ContactClock(150, 100));
        ages.put("bubble", new ContactClock(150, 100)); ages.put("piston", new ContactClock(100, 100)); ages.put("bed", new ContactClock(1000, 100)); ages.put("sulfur", new ContactClock(500, 100));
    }

    public void geometry(List<ServerWorldSnapshot.Cell> cells, int boats, boolean living, boolean ghast) {
        materials = cells == null ? null : List.copyOf(cells);
        frameBoats = boats; frameLiving = living; frameGhast = ghast;
    }

    public void playerPosition(boolean mounted) { if (!mounted) packetCount = 0; }

    public void raw(Object currentMount, double nextX, double nextY, double nextZ, Boolean sulfur) {
        if (!Double.isFinite(nextX) || !Double.isFinite(nextY) || !Double.isFinite(nextZ)) throw new IllegalArgumentException("Vehicle coordinates must be finite.");
        packetCount = increment(packetCount);
        if (mount != currentMount) packetCount = 0;
        mount = currentMount;
        previousDy = dy; dy = nextY - y;
        double dx = nextX - x, dz = nextZ - z;
        horizontal = Math.sqrt(dx * dx + dz * dz);
        x = nextX; y = nextY; z = nextZ;
        allAir = materials == null ? null : materials.stream().allMatch(ServerWorldSnapshot.Cell::air);
        nearbyBoats = materials == null ? null : frameBoats;
        otherLiving = materials == null ? null : frameLiving;
        happyGhast = materials == null ? null : frameGhast;
        ages.get("ice").update(contact(name -> name.equals("ICE") || name.equals("PACKED_ICE") || name.equals("BLUE_ICE") || name.equals("FROSTED_ICE")));
        ages.get("liquid").update(contact(name -> name.contains("WATER") || name.contains("LAVA") || name.contains("BUBBLE")));
        ages.get("slime").update(contact(name -> name.equals("SLIME_BLOCK")));
        ages.get("bubble").update(contact(name -> name.equals("BUBBLE_COLUMN")));
        ages.get("piston").update(contact(name -> name.contains("PISTON")));
        ages.get("bed").update(contact(name -> name.contains("BED") && !name.equals("BEDROCK")));
        ages.get("sulfur").update(sulfur);
        if (allAir == null || happyGhast == null) { airAge = -1; knownAirRun = 0; }
        else if (!allAir || happyGhast) { airAge = 0; knownAirRun = 0; }
        else {
            knownAirRun = increment(knownAirRun);
            if (airAge >= 0) airAge = increment(airAge);
            else if (knownAirRun >= 31) airAge = 31;
        }
    }

    private Boolean contact(java.util.function.Predicate<String> predicate) {
        return materials == null ? null : materials.stream().anyMatch(cell -> predicate.test(cell.material()));
    }

    public void values(Map<String, Double> values) {
        values.keySet().removeIf(key -> key.startsWith("source-mounted-"));
        values.put("source-mounted-x", x); values.put("source-mounted-y", y); values.put("source-mounted-z", z);
        values.put("source-mounted-dy", dy); values.put("source-mounted-previous-dy", previousDy); values.put("source-mounted-horizontal", horizontal);
        values.put("source-mounted-packet-count", (double) packetCount);
        if (airAge >= 0) values.put("source-mounted-air-age", (double) airAge);
        if (allAir != null) values.put("source-mounted-all-air", allAir ? 1d : 0d);
        if (nearbyBoats != null) values.put("source-mounted-nearby-boat-count", (double) nearbyBoats);
        if (otherLiving != null) values.put("source-mounted-other-living-nearby", otherLiving ? 1d : 0d);
        ages.forEach((name, age) -> { if (age.value() != null) values.put("source-mounted-" + name + "-age", age.value().doubleValue()); });
    }

    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }

}
