package io.github.lino9999.linoac.detection.world;

import java.util.List;
import java.util.Objects;

public final class GhostHandContext {
    private GhostHandContext() {}

    public static boolean bed(String material) { return material != null && material.contains("BED") && !material.equals("BEDROCK"); }

    public static Direction direction(String south, String west, String north, String east) {
        if (south == null || west == null || north == null || east == null) return null;
        Direction selected = null;
        if (bed(south)) selected = Direction.SOUTH;
        if (bed(west)) selected = Direction.WEST;
        if (bed(north)) selected = Direction.NORTH;
        if (bed(east)) selected = Direction.EAST;
        return selected;
    }

    public static Enclosure enclosure(List<MaterialSample> materials) {
        if (materials.size() != 10) throw new IllegalArgumentException("A bed enclosure needs ten known material samples.");
        int opaque = 0, glass = 0;
        for (int index = 0; index < materials.size(); index++) {
            MaterialSample material = Objects.requireNonNull(materials.get(index));
            if (material.solid() && material.occluding()) opaque++;
            if (index >= 2 && material.name().contains("GLASS") && !material.name().contains("PANE")) glass++;
        }
        return new Enclosure(opaque, glass);
    }

    public record Offset(int x, int y, int z) {}
    public record MaterialSample(String name, boolean solid, boolean occluding) {
        public MaterialSample { Objects.requireNonNull(name); }
    }
    public record Enclosure(int opaqueBlocks, int glassBlocks) {}

    public enum Direction {
        SOUTH(0, 1), WEST(-1, 0), NORTH(0, -1), EAST(1, 0);
        private final List<Offset> offsets;
        Direction(int x, int z) {
            offsets = List.of(new Offset(0, -1, 0), new Offset(x, -1, z), new Offset(-x, 0, -z),
                    new Offset(x == 0 ? -1 : 0, 0, x == 0 ? 0 : 1), new Offset(x == 0 ? 1 : 0, 0, x == 0 ? 0 : -1),
                    new Offset(x == 0 ? -1 : x, 0, x == 0 ? z : 1), new Offset(x == 0 ? 1 : x, 0, x == 0 ? z : -1),
                    new Offset(x * 2, 0, z * 2), new Offset(0, 1, 0), new Offset(x, 1, z));
        }
        public List<Offset> offsets() { return offsets; }
    }
}
