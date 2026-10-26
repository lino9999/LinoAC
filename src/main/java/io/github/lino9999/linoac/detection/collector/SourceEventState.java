package io.github.lino9999.linoac.detection.collector;

import java.util.Map;

public final class SourceEventState {
    private static final Age[] AGE_TYPES = Age.values();
    private final int[] ages = new int[AGE_TYPES.length];
    private final long[] ticks = new long[Tick.values().length];
    private int previousPlacementAmount;
    private int previousPlacementSlot;

    public SourceEventState() { for (Age age : AGE_TYPES) ages[age.ordinal()] = age.initial; }
    public void flying() { for (int i = 0; i < ages.length; i++) if (ages[i] != Integer.MAX_VALUE) ages[i]++; }
    public void mark(Age age) { ages[age.ordinal()] = 0; }
    public int age(Age age) { return ages[age.ordinal()]; }
    public void mark(Tick event, long tick) { ticks[event.ordinal()] = tick; }
    public long elapsed(Tick event, long now) { return now - ticks[event.ordinal()]; }

    public void placement(String material, boolean cancelled, int amount, int slot) {
        mark(Age.BLOCK_PLACE);
        if (cancelled || amount == previousPlacementAmount && slot == previousPlacementSlot) mark(Age.CANCELLED_PLACE);
        previousPlacementAmount = amount;
        previousPlacementSlot = slot;
        if (java.util.Set.of("ICE", "PACKED_ICE", "BLUE_ICE", "FROSTED_ICE").contains(material)) mark(Age.PLACED_ICE);
        if (material.equals("COBWEB")) mark(Age.PLACED_WEB);
        if (material.contains("SLIME")) mark(Age.PLACED_SLIME);
        if (material.equals("LADDER") || material.contains("VINE") || material.equals("SCAFFOLDING")) mark(Age.PLACED_CLIMBABLE);
        if (material.contains("SOUL")) mark(Age.PLACED_SOUL);
    }

    public void damage(String type) {
        switch (type) {
            case "RAVAGER" -> mark(Age.RAVAGER_DAMAGE);
            case "END_CRYSTAL", "ENDER_CRYSTAL" -> mark(Age.CRYSTAL_DAMAGE);
            case "ENDER_DRAGON" -> mark(Age.DRAGON_DAMAGE);
            case "IRON_GOLEM" -> mark(Age.GOLEM_DAMAGE);
            case "HOGLIN" -> mark(Age.HOGLIN_DAMAGE);
            case "FIREBALL", "SMALL_FIREBALL", "DRAGON_FIREBALL" -> mark(Age.FIREBALL_DAMAGE);
            default -> { }
        }
    }

    public void damageCause(String cause) {
        if (cause == null) return;
        mark(Age.ALL_DAMAGE);
        if (!cause.equals("FALL") && !cause.equals("SUFFOCATION")) mark(Age.NONFALL_DAMAGE);
        if (cause.equals("ENTITY_ATTACK")) mark(Age.MELEE_DAMAGE);
        if (cause.equals("FALL")) mark(Age.FALL_DAMAGE);
        if (cause.equals("CONTACT")) mark(Age.CONTACT_DAMAGE);
        if (cause.equals("PROJECTILE")) mark(Age.PROJECTILE_DAMAGE);
    }

    public void values(Map<String, Double> values, long tick) {
        for (Age age : AGE_TYPES) values.put(age.metric, (double) age(age));
        values.put("protocol-exempt-WORLD_CHANGE", age(Age.WORLD_CHANGE) < 40 ? 1d : 0d);
        values.put("protocol-exempt-FALL_DAMAGE", age(Age.FALL_DAMAGE) < 40 ? 1d : 0d);
        values.put("protocol-exempt-DRAGON_DAMAGE", age(Age.DRAGON_DAMAGE) < 60 ? 1d : 0d);
        values.put("protocol-exempt-FIREBALL", age(Age.FIREBALL_DAMAGE) < 25 ? 1d : 0d);
        values.put("protocol-exempt-PLACED_WEB", age(Age.PLACED_WEB) < 25 ? 1d : 0d);
        values.put("protocol-exempt-PLACED_SLIME", age(Age.PLACED_SLIME) < 120 ? 1d : 0d);
        values.put("protocol-exempt-CHORUS_FRUIT", age(Age.CHORUS_FRUIT) < 25 ? 1d : 0d);
        values.put("protocol-exempt-FISHING_ROD", age(Age.FISHING_ROD) < 100 ? 1d : 0d);
        values.put("protocol-exempt-BLOCK_PLACE", age(Age.BLOCK_PLACE) < 30 ? 1d : 0d);
        values.put("protocol-exempt-BLOCK_PLACE_FAST", age(Age.BLOCK_PLACE) < 15 ? 1d : 0d);
        values.put("protocol-exempt-PLACING", age(Age.BLOCK_PLACE) < 10 ? 1d : 0d);
        values.put("protocol-exempt-ATTACK_DAMAGE", age(Age.MELEE_DAMAGE) < 80 ? 1d : 0d);
        values.put("protocol-exempt-PLACED_CLIMBABLE", age(Age.PLACED_CLIMBABLE) < 30 ? 1d : 0d);
        values.put("protocol-exempt-CANCELLED_BREAK", elapsed(Tick.CANCELLED_BREAK, tick) < 40 ? 1d : 0d);
        values.put("protocol-exempt-BLOCK_BREAK", age(Age.BLOCK_BREAK) < 25 ? 1d : 0d);
        values.put("protocol-exempt-CANCELLED_PLACE", age(Age.CANCELLED_PLACE) < 40 ? 1d : 0d);
        values.put("protocol-exempt-CANCELLED_MOVE", age(Age.CANCELLED_MOVE) < 5 ? 1d : 0d);
        values.put("protocol-exempt-EMPTIED_BUCKET", age(Age.EMPTIED_BUCKET) < 15 ? 1d : 0d);
        values.put("protocol-exempt-RESPAWN", elapsed(Tick.RESPAWN, tick) < 20 ? 1d : 0d);
    }

    public enum Tick { RESPAWN, BED_ENTER, BED_LEAVE, CANCELLED_BREAK }
    public enum Age {
        DEATH(100, "death-movement-age"), WORLD_CHANGE(100, "source-world-change-movement-age"), NONPEARL_TELEPORT(100, "protocol-nonpearl-teleport-age"),
        ENDER_PEARL(100, "protocol-ender-pearl-age"), PLACED_ICE(100, "source-placed-ice-movement-age"), RAVAGER_DAMAGE(1000, "source-ravager-damage-movement-age"),
        CRYSTAL_DAMAGE(100, "source-crystal-damage-movement-age"), DRAGON_DAMAGE(100, "source-dragon-damage-movement-age"), GOLEM_DAMAGE(0, "source-golem-damage-movement-age"),
        HOGLIN_DAMAGE(0, "source-hoglin-damage-movement-age"), ALL_DAMAGE(100, "source-all-damage-age"), NONFALL_DAMAGE(100, "source-nonfall-damage-movement-age"), MELEE_DAMAGE(100, "source-melee-damage-movement-age"),
        FALL_DAMAGE(100, "source-fall-damage-movement-age"), PLACED_WEB(100, "source-placed-web-movement-age"), CHORUS_FRUIT(100, "source-chorus-fruit-movement-age"),
        FIREBALL_DAMAGE(100, "source-fireball-damage-movement-age"), BLOCK_PLACE(100, "source-block-place-movement-age"), PLACED_SLIME(100, "source-placed-slime-movement-age"),
        FISHING_ROD(1000, "source-fishing-rod-movement-age"), EMPTIED_BUCKET(100, "source-emptied-bucket-movement-age"), CANCELLED_MOVE(1000, "source-cancelled-move-movement-age"),
        BLOCK_BREAK(100, "source-block-break-movement-age"), CANCELLED_PLACE(100, "source-cancelled-place-movement-age"), PLACED_CLIMBABLE(100, "source-placed-climbable-movement-age"),
        PLACED_SOUL(100, "source-placed-soul-movement-age"), CONTACT_DAMAGE(0, "source-contact-damage-movement-age"),
        PROJECTILE_DAMAGE(0, "source-projectile-damage-movement-age");

        private final int initial;
        private final String metric;
        Age(int initial, String metric) { this.initial = initial; this.metric = metric; }
    }
}
