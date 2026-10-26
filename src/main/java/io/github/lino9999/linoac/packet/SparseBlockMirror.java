package io.github.lino9999.linoac.packet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SparseBlockMirror {
    private final int maximumChunks, maximumBlocks;
    private final Map<Long, Set<Block>> chunks = new HashMap<>();
    private final Set<Long> covered = new HashSet<>();
    private int blocks;
    private boolean complete = true;

    public SparseBlockMirror(int maximumChunks, int maximumBlocks) {
        if (maximumChunks < 1 || maximumBlocks < 1) throw new IllegalArgumentException("Mirror limits must be positive.");
        this.maximumChunks = maximumChunks; this.maximumBlocks = maximumBlocks;
    }

    public void replace(int chunkX, int chunkZ, Set<Block> entries) {
        if (!complete) return;
        Set<Block> replacement = Set.copyOf(entries);
        if (replacement.stream().anyMatch(block -> block.x() >> 4 != chunkX || block.z() >> 4 != chunkZ)) { invalidate(); return; }
        long key = key(chunkX, chunkZ);
        Set<Block> previous = chunks.get(key);
        int nextBlocks = blocks - (previous == null ? 0 : previous.size()) + replacement.size();
        if (previous == null && chunks.size() == maximumChunks || nextBlocks > maximumBlocks) { invalidate(); return; }
        chunks.put(key, new HashSet<>(replacement)); blocks = nextBlocks;
        covered.add(key);
    }

    public void change(Block block, boolean present) {
        if (!complete) return;
        long key = key(block.x() >> 4, block.z() >> 4);
        Set<Block> previous = chunks.get(key);
        if (previous == null && !present || previous != null && previous.contains(block) == present) return;
        if (previous == null && chunks.size() == maximumChunks || present && blocks == maximumBlocks) { invalidate(); return; }
        if (previous == null) { previous = new HashSet<>(); chunks.put(key, previous); }
        if (present) { previous.add(block); blocks++; } else { previous.remove(block); blocks--; }
    }

    public void unload(int chunkX, int chunkZ) {
        Set<Block> previous = chunks.remove(key(chunkX, chunkZ));
        covered.remove(key(chunkX, chunkZ));
        if (previous != null) blocks -= previous.size();
    }

    public Boolean below(double x, double y, double z) {
        if (!complete || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000) return null;
        int minChunkX = ((int) Math.floor(x - 3.5)) >> 4, maxChunkX = ((int) Math.floor(x + 3.5)) >> 4;
        int minChunkZ = ((int) Math.floor(z - 3.5)) >> 4, maxChunkZ = ((int) Math.floor(z + 3.5)) >> 4;
        boolean known = true;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            Set<Block> entries = chunks.get(key(cx, cz));
            if (!covered.contains(key(cx, cz))) known = false;
            if (entries == null) continue;
            for (Block block : entries) {
                double dx = x - (block.x() + .5), dz = z - (block.z() + .5), dy = y - (block.y() + .5);
                if (dx * dx + dz * dz < 12.25 && dy >= 0 && dy < 20) return true;
            }
        }
        return known ? Boolean.FALSE : null;
    }

    public void invalidate() { complete = false; chunks.clear(); covered.clear(); blocks = 0; }
    public void reset() { complete = true; chunks.clear(); covered.clear(); blocks = 0; }
    public int chunks() { return chunks.size(); }
    public int blocks() { return blocks; }
    public boolean complete() { return complete; }
    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    public record Block(int x, int y, int z) {}
}
