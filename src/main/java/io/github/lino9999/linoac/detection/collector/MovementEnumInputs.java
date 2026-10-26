package io.github.lino9999.linoac.detection.collector;

import java.util.Map;

public final class MovementEnumInputs {
    private MovementEnumInputs() {}

    public static void populate(Map<String, Double> values, boolean initialWorldStateKnown) {
        Boolean modern = binary(values, "protocol-server-at-least-1-9");
        Boolean firework = Boolean.FALSE.equals(modern) ? Boolean.FALSE
                : Boolean.TRUE.equals(modern) ? younger(values, "source-firework-position-age", 150) : null;
        if (firework == null) values.remove("protocol-exempt-FIREWORK");
        else values.put("protocol-exempt-FIREWORK", firework ? 1d : 0d);

        Boolean campfire = contact(values, "source-campfire-contact", initialWorldStateKnown);
        if (campfire == null) values.remove("protocol-exempt-CAMPFIRE");
        else values.put("protocol-exempt-CAMPFIRE", campfire ? 1d : 0d);

        Boolean flowerPot = contact(values, "source-flower-pot-contact", initialWorldStateKnown);
        if (flowerPot == null) values.remove("protocol-exempt-FLOWER_POT");
        else values.put("protocol-exempt-FLOWER_POT", flowerPot ? 1d : 0d);

        Boolean projectile = younger(values, "source-projectile-damage-movement-age", 20);
        if (projectile == null) values.remove("protocol-exempt-PROJECTILE_DAMAGE");
        else values.put("protocol-exempt-PROJECTILE_DAMAGE", projectile ? 1d : 0d);
    }

    private static Boolean contact(Map<String, Double> values, String key, boolean initialWorldStateKnown) {
        return !values.containsKey(key) && initialWorldStateKnown ? Boolean.FALSE : binary(values, key);
    }

    private static Boolean binary(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null ? null : value == 0 ? Boolean.FALSE : value == 1 ? Boolean.TRUE : null;
    }

    private static Boolean younger(Map<String, Double> values, String key, int maximum) {
        Double age = values.get(key);
        return age == null || !Double.isFinite(age) || age < 0 || age != Math.rint(age) ? null : age < maximum;
    }
}
