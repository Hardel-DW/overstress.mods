package fr.hardel.overstress.fakeplayer.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.OptionalInt;

final class ClientTerrain {
    private static final double HALF_WIDTH = EntityTypes.PLAYER.getWidth() / 2;
    private final Long2ObjectMap<long[]> surfaces = new Long2ObjectOpenHashMap<>();
    private int minY;
    private int maxY;
    private int bits;

    void enter(DimensionType dimension) {
        surfaces.clear();
        minY = dimension.minY();
        maxY = dimension.minY() + dimension.height() - 1;
        bits = Mth.ceillog2(dimension.height() + 1);
    }

    void receive(ClientboundLevelChunkWithLightPacket chunk) {
        surfaces.put(ChunkPos.pack(chunk.x(), chunk.z()), chunk.chunkData().getHeightmaps().get(Heightmap.Types.MOTION_BLOCKING));
    }

    void forget(ChunkPos pos) {
        surfaces.remove(pos.pack());
    }

    boolean holds(double x, double z) {
        return surfaces.containsKey(ChunkPos.pack(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)));
    }

    boolean aboveBuildHeight(double y) {
        return y > maxY;
    }

    int minY() {
        return minY;
    }

    OptionalInt top(double x, double z) {
        int top = minY;
        for (int blockX = Mth.floor(x - HALF_WIDTH); blockX < Mth.ceil(x + HALF_WIDTH); blockX++) {
            for (int blockZ = Mth.floor(z - HALF_WIDTH); blockZ < Mth.ceil(z + HALF_WIDTH); blockZ++) {
                long[] surface = surfaces.get(ChunkPos.pack(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ)));
                if (surface == null) {
                    return OptionalInt.empty();
                }

                top = Math.max(top, new SimpleBitStorage(bits, 256, surface).get((blockX & 15) + (blockZ & 15) * 16) + minY);
            }
        }

        return OptionalInt.of(top);
    }
}
