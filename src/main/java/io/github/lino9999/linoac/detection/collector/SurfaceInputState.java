package io.github.lino9999.linoac.detection.collector;

import java.util.Map;

public final class SurfaceInputState {
    private final ContactClock support = new ContactClock(100, 100);
    private final ContactClock boats = new ContactClock(100, 100);
    private final ContactClock swimming = new ContactClock(100, 100);
    private final ContactClock slab = new ContactClock(0, 100);
    private final ContactClock liquid = new ContactClock(100, 100);
    private final ContactClock anyGliding = new ContactClock(100, 30);
    private final ContactClock speedAbsent = new ContactClock(100, 20);
    private Double retainedGlideHorizontal = 0d;
    private int climbReady;
    private boolean climbKnown = true;

    public void position(Map<String, Double> facts, Boolean swimmingActive) {
        support.update(bit(facts, "source-center-support-solid"));
        boats.update(bit(facts, "source-nearby-boat-contact"));
        swimming.update(swimmingActive);
        slab.update(bit(facts, "source-slab-contact"));
        liquid.update(bit(facts, "source-liquid-contact"));
        Boolean ready = bit(facts, "source-climb-ready");
        if (ready == null) { climbKnown = false; climbReady = 0; }
        else if (!ready) { climbReady = 0; climbKnown = true; }
        else {
            climbReady = climbReady == Integer.MAX_VALUE ? climbReady : climbReady + 1;
            if (climbReady >= 4) climbKnown = true;
        }
    }

    public void glidePosition(boolean previousEquipmentGliding, Boolean logicalGliding, boolean physicalGliding, double horizontal, Boolean speedActive) {
        anyGliding.update(previousEquipmentGliding || Boolean.TRUE.equals(logicalGliding) ? Boolean.TRUE : logicalGliding == null ? null : Boolean.FALSE);
        speedAbsent.update(speedActive);
        if (physicalGliding || Boolean.TRUE.equals(logicalGliding)) retainedGlideHorizontal = horizontal;
        else if (logicalGliding == null) retainedGlideHorizontal = null;
    }

    public void values(Map<String, Double> values) {
        put(values, "source-solid-support-position-age", support.value());
        put(values, "source-nearby-boat-position-age", boats.value());
        put(values, "source-swimming-contact-position-age", swimming.value());
        put(values, "source-climb-ready-position-count", climbKnown ? climbReady : null);
        put(values, "source-slab-contact-position-age", slab.value());
        put(values, "source-liquid-contact-position-age", liquid.value());
        put(values, "source-any-gliding-position-age", anyGliding.value());
        put(values, "source-speed-absent-position-age", speedAbsent.value());
        if (retainedGlideHorizontal == null) values.remove("source-retained-glide-horizontal"); else values.put("source-retained-glide-horizontal", retainedGlideHorizontal);
    }

    public void exemptions(Map<String, Double> values, boolean supportedSwimming, boolean oldServer) {
        Integer swimAge = swimming.value();
        if (!supportedSwimming) values.put("protocol-exempt-SWIMMING_JESUS", 0d);
        else if (swimAge != null) values.put("protocol-exempt-SWIMMING_JESUS", swimAge < 5 ? 1d : 0d);
        else values.remove("protocol-exempt-SWIMMING_JESUS");
        Double clientFlag = values.get("client-at-least-1-13");
        Boolean modernClient = clientFlag == null ? null : clientFlag == 1 ? Boolean.TRUE : clientFlag == 0 ? Boolean.FALSE : null;
        Integer liquidAge = liquid.value();
        if (!oldServer || Boolean.FALSE.equals(modernClient) || liquidAge != null && liquidAge >= 60) values.put("protocol-exempt-SWIMMING_ON_OLD_VERSION", 0d);
        else if (Boolean.TRUE.equals(modernClient) && liquidAge != null) values.put("protocol-exempt-SWIMMING_ON_OLD_VERSION", 1d);
        else values.remove("protocol-exempt-SWIMMING_ON_OLD_VERSION");
    }

    private static Boolean bit(Map<String, Double> values, String key) { return values.containsKey(key) ? values.get(key) == 1 : null; }
    private static void put(Map<String, Double> values, String key, Integer value) { if (value == null) values.remove(key); else values.put(key, value.doubleValue()); }
}
