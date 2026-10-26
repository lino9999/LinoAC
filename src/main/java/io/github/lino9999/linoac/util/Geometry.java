package io.github.lino9999.linoac.util;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class Geometry {
    private Geometry() {}

    public static double distance(Vector point, BoundingBox box) {
        double x = Math.max(box.getMinX() - point.getX(), Math.max(0, point.getX() - box.getMaxX()));
        double y = Math.max(box.getMinY() - point.getY(), Math.max(0, point.getY() - box.getMaxY()));
        double z = Math.max(box.getMinZ() - point.getZ(), Math.max(0, point.getZ() - box.getMaxZ()));
        return Math.sqrt(x * x + y * y + z * z);
    }
}
