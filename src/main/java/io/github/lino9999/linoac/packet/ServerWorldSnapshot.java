package io.github.lino9999.linoac.packet;

public record ServerWorldSnapshot(int nearbyEntities, boolean complete, boolean cobweb, int airBelow, boolean twoBelowAir,
                                  java.util.List<Cell> cells, double borderCenterX, double borderCenterZ, double borderSize,
                                  long generation, boolean waterlogged, boolean thirdBelowSlime, Nearby nearby,
                                  Boolean lowerPlaneAir, Boolean thirdBelowAir, java.util.Map<String, Double> materialFacts, String secondBelowMaterial) {
    public static java.util.Set<String> materialFactKeys() { return WorldMaterialFacts.keys(); }
    public ServerWorldSnapshot {
        if (nearbyEntities < -1 || complete && (nearbyEntities < 0 || airBelow < 0 || airBelow > 9)) throw new IllegalArgumentException("Invalid server world snapshot.");
        cells = java.util.List.copyOf(cells);
        nearby = java.util.Objects.requireNonNull(nearby);
        if (!cells.isEmpty() && cells.size() != 36) throw new IllegalArgumentException("A complete material grid needs 36 cells.");
        materialFacts = complete ? WorldMaterialFacts.sample(cells, twoBelowAir, thirdBelowSlime, lowerPlaneAir, thirdBelowAir, secondBelowMaterial) : java.util.Map.of();
    }
    public ServerWorldSnapshot(int nearbyEntities, boolean complete, boolean cobweb, int airBelow, boolean twoBelowAir,
                               java.util.List<Cell> cells, double borderCenterX, double borderCenterZ, double borderSize,
                               long generation, boolean waterlogged, boolean thirdBelowSlime, Nearby nearby,
                               Boolean lowerPlaneAir, Boolean thirdBelowAir, java.util.Map<String, Double> materialFacts) {
        this(nearbyEntities, complete, cobweb, airBelow, twoBelowAir, cells, borderCenterX, borderCenterZ, borderSize,
                generation, waterlogged, thirdBelowSlime, nearby, lowerPlaneAir, thirdBelowAir, materialFacts, null);
    }
    public ServerWorldSnapshot(int nearbyEntities, boolean complete, boolean cobweb, int airBelow, boolean twoBelowAir,
                               java.util.List<Cell> cells, double borderCenterX, double borderCenterZ, double borderSize,
                               long generation, boolean waterlogged, boolean thirdBelowSlime, Nearby nearby) {
        this(nearbyEntities, complete, cobweb, airBelow, twoBelowAir, cells, borderCenterX, borderCenterZ, borderSize, generation, waterlogged, thirdBelowSlime, nearby, null, null, null);
    }
    public ServerWorldSnapshot(int nearbyEntities, boolean complete, boolean cobweb, int airBelow, boolean twoBelowAir,
                               java.util.List<Cell> cells, double borderCenterX, double borderCenterZ, double borderSize,
                               long generation, boolean waterlogged, boolean thirdBelowSlime, Nearby nearby, Boolean lowerPlaneAir, Boolean thirdBelowAir) {
        this(nearbyEntities, complete, cobweb, airBelow, twoBelowAir, cells, borderCenterX, borderCenterZ, borderSize, generation, waterlogged, thirdBelowSlime, nearby, lowerPlaneAir, thirdBelowAir, null);
    }
    public ServerWorldSnapshot(int nearbyEntities, boolean complete, boolean cobweb, int airBelow, boolean twoBelowAir,
                               java.util.List<Cell> cells, double borderCenterX, double borderCenterZ, double borderSize,
                               long generation, boolean waterlogged, boolean thirdBelowSlime, boolean nearbyLiving) {
        this(nearbyEntities, complete, cobweb, airBelow, twoBelowAir, cells, borderCenterX, borderCenterZ, borderSize, generation, waterlogged, thirdBelowSlime, new Nearby(nearbyLiving, false, false, false));
    }
    public ServerWorldSnapshot(int nearbyEntities, boolean complete, boolean cobweb, int airBelow, boolean twoBelowAir) {
        this(nearbyEntities, complete, cobweb, airBelow, twoBelowAir, java.util.List.of(), Double.NaN, Double.NaN, Double.NaN, 0, false, false, false);
    }
    public ServerWorldSnapshot(int nearbyEntities, boolean complete, boolean cobweb, int airBelow, boolean twoBelowAir,
                               java.util.List<Cell> cells, double borderCenterX, double borderCenterZ, double borderSize) {
        this(nearbyEntities, complete, cobweb, airBelow, twoBelowAir, cells, borderCenterX, borderCenterZ, borderSize, 0, false, false, false);
    }
    public boolean geometryKnown() { return complete && cells.size() == 36; }
    public boolean airVolume27() { return geometryKnown() && java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 < 9).allMatch(i -> cells.get(i).air()); }
    public boolean solidCenter() { return geometryKnown() && cells.get(16).solid() && cells.get(19).solid(); }
    public boolean bodyObstructed() { return geometryKnown() && java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 >= 3 && i % 12 < 9).anyMatch(i -> !cells.get(i).air()); }
    public boolean ceilingObstructed() { return geometryKnown() && java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 >= 9).anyMatch(i -> !cells.get(i).air()); }
    public boolean anyMaterial(java.util.function.Predicate<String> predicate) { return geometryKnown() && cells.stream().anyMatch(cell -> predicate.test(cell.material())); }
    public boolean planeMaterial(int lowerOffset, int upperOffset, java.util.function.Predicate<String> predicate) {
        return geometryKnown() && java.util.stream.IntStream.range(0, 36).filter(i -> i % 12 >= lowerOffset && i % 12 < upperOffset).anyMatch(i -> predicate.test(cells.get(i).material()));
    }
    public boolean borderKnown() { return Double.isFinite(borderCenterX) && Double.isFinite(borderCenterZ) && Double.isFinite(borderSize) && borderSize > 0; }
    public boolean nearbyLiving() { return nearby.living(); }
    public boolean nearBorder(double x, double z) {
        return borderKnown() && nearBorder(x, z, borderCenterX, borderCenterZ, borderSize);
    }
    public static boolean nearBorder(double x, double z, double centerX, double centerZ, double size) {
        double half = size / 2;
        return Double.isFinite(centerX) && Double.isFinite(centerZ) && Double.isFinite(size) && size > 0
                && Math.min(Math.min(Math.abs(x - centerX - half), Math.abs(x - centerX + half)), Math.min(Math.abs(z - centerZ - half), Math.abs(z - centerZ + half))) < 1;
    }
    public static ServerWorldSnapshot unknown(int nearbyEntities) { return new ServerWorldSnapshot(nearbyEntities, false, false, -1, false); }
    public record Cell(String material, boolean solid, boolean air) {
        public Cell { java.util.Objects.requireNonNull(material); }
    }
    public record Nearby(boolean living, boolean boat, boolean shulker, boolean happyGhast) {}
}
