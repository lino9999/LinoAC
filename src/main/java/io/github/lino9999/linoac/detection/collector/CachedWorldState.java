package io.github.lino9999.linoac.detection.collector;

public final class CachedWorldState {
    private int bubbleAge = 100;
    private int slimeBelowAge = 100;
    private int ceilingAge = 100;
    private int waterloggedAge = 1000;
    private long generation = -1;
    private boolean geometryKnown;
    private boolean hadKnownGeometry;
    private boolean waterloggedKnown;
    private boolean contactsKnown;
    private int iceAge, trapdoorAge = 100, fenceAge = 100, bedAge = 100, soulSpeedAge = 100;
    private int bodySlimeAge = 500, bodySlabAge = 100, nearbyLivingAge = 100;
    private int honeyAge = 100, iceContactAge = 100, shulkerBoxAge = 100, scaffoldingAge = 100, farmlandAge = 100, shulkerAge = 100, happyGhastAge = 100;
    private int pistonAge = 100;

    public void observe(long sampleGeneration, boolean waterlogged) {
        if (sampleGeneration < 0) throw new IllegalArgumentException("World generation must be nonnegative.");
        waterloggedKnown = true;
        if (sampleGeneration != generation) {
            if (waterlogged) waterloggedAge = 0;
            generation = sampleGeneration;
        }
    }

    public void position(boolean known, boolean bubble, boolean slimeBelow, boolean ceiling) {
        geometryKnown = known;
        if (known) {
            hadKnownGeometry = true;
            bubbleAge = bubble ? 0 : increment(bubbleAge);
            slimeBelowAge = slimeBelow ? 0 : increment(slimeBelowAge);
            ceilingAge = ceiling ? 0 : increment(ceilingAge);
        } else if (!hadKnownGeometry) {
            bubbleAge = increment(bubbleAge);
            slimeBelowAge = increment(slimeBelowAge);
            ceilingAge = increment(ceilingAge);
        }
        waterloggedAge = increment(waterloggedAge);
    }

    public void contacts(Contacts contacts) {
        contactsKnown = contacts != null;
        if (contacts == null) return;
        iceAge = contacts.ice ? 0 : increment(iceAge);
        trapdoorAge = contacts.trapdoor ? 0 : increment(trapdoorAge);
        fenceAge = contacts.fence ? 0 : increment(fenceAge);
        bedAge = contacts.bed ? 0 : increment(bedAge);
        soulSpeedAge = contacts.soulSpeed ? 0 : increment(soulSpeedAge);
        bodySlimeAge = contacts.bodySlime ? 0 : increment(bodySlimeAge);
        bodySlabAge = contacts.bodySlab ? 0 : increment(bodySlabAge);
        nearbyLivingAge = contacts.nearbyLiving ? 0 : increment(nearbyLivingAge);
        honeyAge = contacts.honey ? 0 : increment(honeyAge);
        iceContactAge = contacts.iceContact ? 0 : increment(iceContactAge);
        shulkerBoxAge = contacts.shulkerBox ? 0 : increment(shulkerBoxAge);
        scaffoldingAge = contacts.scaffolding ? 0 : increment(scaffoldingAge);
        farmlandAge = contacts.farmland ? 0 : increment(farmlandAge);
        shulkerAge = contacts.shulker ? 0 : increment(shulkerAge);
        happyGhastAge = contacts.happyGhast ? 0 : increment(happyGhastAge);
        pistonAge = contacts.piston ? 0 : increment(pistonAge);
    }

    public Snapshot snapshot() { return new Snapshot(geometryKnown, waterloggedKnown, bubbleAge, slimeBelowAge, ceilingAge, waterloggedAge,
            contactsKnown, iceAge, trapdoorAge, fenceAge, bedAge, soulSpeedAge, bodySlimeAge, bodySlabAge, nearbyLivingAge,
            honeyAge, iceContactAge, shulkerBoxAge, scaffoldingAge, farmlandAge, shulkerAge, happyGhastAge, pistonAge, !hadKnownGeometry); }
    private static int increment(int value) { return value == Integer.MAX_VALUE ? value : value + 1; }
    public record Contacts(boolean ice, boolean trapdoor, boolean fence, boolean bed, boolean soulSpeed, boolean bodySlime, boolean bodySlab, boolean nearbyLiving,
                           boolean honey, boolean iceContact, boolean shulkerBox, boolean scaffolding, boolean farmland, boolean shulker, boolean happyGhast, boolean piston) {
        private static final java.util.Set<String> REQUIRED = java.util.Set.of("source-ice-below-contact", "source-trapdoor-contact", "source-fence-contact", "source-bed-contact", "source-body-slime-contact", "source-body-slab-contact", "source-nearby-living-contact",
                "source-honey-contact", "source-ice-contact", "source-shulker-box-contact", "source-scaffolding-contact", "source-farmland-contact", "source-nearby-shulker-contact", "source-nearby-happy-ghast-contact", "source-piston-contact");
        public Contacts(boolean ice, boolean trapdoor, boolean fence, boolean bed, boolean soulSpeed, boolean bodySlime, boolean bodySlab, boolean nearbyLiving) {
            this(ice, trapdoor, fence, bed, soulSpeed, bodySlime, bodySlab, nearbyLiving, false, false, false, false, false, false, false, false);
        }
        public static Contacts from(java.util.Map<String, Double> facts, boolean soulSpeed) {
            if (!facts.keySet().containsAll(REQUIRED)) return null;
            return new Contacts(facts.get("source-ice-below-contact") == 1, facts.get("source-trapdoor-contact") == 1, facts.get("source-fence-contact") == 1,
                    facts.get("source-bed-contact") == 1, soulSpeed, facts.get("source-body-slime-contact") == 1, facts.get("source-body-slab-contact") == 1, facts.get("source-nearby-living-contact") == 1,
                    facts.get("source-honey-contact") == 1, facts.get("source-ice-contact") == 1, facts.get("source-shulker-box-contact") == 1, facts.get("source-scaffolding-contact") == 1,
                    facts.get("source-farmland-contact") == 1, facts.get("source-nearby-shulker-contact") == 1, facts.get("source-nearby-happy-ghast-contact") == 1, facts.get("source-piston-contact") == 1);
        }
    }
    public record Snapshot(boolean geometryKnown, boolean waterloggedKnown, int bubbleAge, int slimeBelowAge, int ceilingAge, int waterloggedAge,
                           boolean contactsKnown, int iceAge, int trapdoorAge, int fenceAge, int bedAge, int soulSpeedAge, int bodySlimeAge, int bodySlabAge, int nearbyLivingAge,
                           int honeyAge, int iceContactAge, int shulkerBoxAge, int scaffoldingAge, int farmlandAge, int shulkerAge, int happyGhastAge, int pistonAge, boolean initialStateKnown) {
        public Double boatContact(Double measured) { return measured != null ? measured : initialStateKnown ? Double.valueOf(0) : null; }
    }
}
