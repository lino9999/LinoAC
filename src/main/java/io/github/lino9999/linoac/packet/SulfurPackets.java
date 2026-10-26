package io.github.lino9999.linoac.packet;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.GlobalPalette;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SulfurPackets {
    private static final int MAXIMUM_BLOCKS = 131072;
    private SulfurPackets() {}

    public static Chunk capture(Column column, int minimumY, int height, ClientVersion version) {
        if (!column.isFullChunk() || minimumY % 16 != 0 || height <= 0 || height % 16 != 0 || height > 65536
                || column.getChunks().length != height / 16 || Math.abs((long) column.getX()) > 1875000 || Math.abs((long) column.getZ()) > 1875000) {
            throw new IllegalArgumentException("Chunk coverage or dimension geometry is incomplete.");
        }
        Set<SparseBlockMirror.Block> blocks = new HashSet<>();
        Map<Integer, Boolean> states = new HashMap<>();
        var sections = column.getChunks();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            var section = sections[sectionIndex];
            if (section == null) throw new IllegalArgumentException("Chunk section is missing.");
            if (section instanceof Chunk_v1_18 modern && !(modern.getChunkData().palette instanceof GlobalPalette)) {
                var palette = modern.getChunkData().palette;
                boolean candidate = false;
                for (int index = 0; index < palette.size(); index++) if (sulfur(palette.idToState(index), version, states)) { candidate = true; break; }
                if (!candidate) continue;
            }
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (!sulfur(section.getBlockId(x, y, z), version, states)) continue;
                if (blocks.size() == MAXIMUM_BLOCKS) throw new IllegalArgumentException("Chunk candidate limit exceeded.");
                blocks.add(new SparseBlockMirror.Block((column.getX() << 4) + x, minimumY + sectionIndex * 16 + y, (column.getZ() << 4) + z));
            }
        }
        return new Chunk(column.getX(), column.getZ(), blocks);
    }

    private static boolean sulfur(int state, ClientVersion version, Map<Integer, Boolean> states) {
        return states.computeIfAbsent(state, key -> WrappedBlockState.getByGlobalId(version, key, false).getType() == StateTypes.POTENT_SULFUR);
    }

    public record Chunk(int x, int z, Set<SparseBlockMirror.Block> blocks) {
        public Chunk { blocks = Set.copyOf(blocks); }
    }
    public record Change(SparseBlockMirror.Block block, boolean present) {}
}
