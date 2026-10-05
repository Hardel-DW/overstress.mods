package fr.hardel.overstress.fakeplayer.client;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

public final class ClientTerrain {
    private static final double HALF_WIDTH = EntityTypes.PLAYER.getWidth() / 2;
    private static final int COLUMNS = SectionPos.SECTION_SIZE * SectionPos.SECTION_SIZE;
    private final Long2ObjectMap<ChunkSurface> surfaces = new Long2ObjectOpenHashMap<>();
    private final LevelChunkSection section;
    private int minY;
    private int maxY;
    private int bits;

    ClientTerrain(RegistryAccess registries) {
        this.section = new LevelChunkSection(PalettedContainerFactory.create(registries));
    }

    public OptionalInt height(int blockX, int blockZ) {
        ChunkSurface surface = surface(blockX, blockZ);
        int column = column(blockX, blockZ);
        return surface == null || !surface.known(column) ? OptionalInt.empty() : OptionalInt.of(surface.height(column) + minY);
    }

    public @Nullable BlockState top(int blockX, int blockZ) {
        ChunkSurface surface = surface(blockX, blockZ);
        return surface == null ? null : surface.top(column(blockX, blockZ));
    }

    public @Nullable BlockState block(BlockPos pos) {
        OptionalInt height = height(pos.getX(), pos.getZ());
        return height.isPresent() && height.getAsInt() - 1 == pos.getY() ? top(pos.getX(), pos.getZ()) : null;
    }

    void enter(DimensionType dimension) {
        surfaces.clear();
        minY = dimension.minY();
        maxY = dimension.minY() + dimension.height() - 1;
        bits = Mth.ceillog2(dimension.height() + 1);
    }

    void receive(ClientboundLevelChunkWithLightPacket chunk) {
        SimpleBitStorage heights = new SimpleBitStorage(bits, COLUMNS, chunk.chunkData().getHeightmaps().get(Heightmap.Types.MOTION_BLOCKING));
        char[] tops = new char[COLUMNS];
        char[] belows = new char[COLUMNS];
        int highest = highestTop(heights);
        FriendlyByteBuf buffer = chunk.chunkData().getReadBuffer();
        for (int bottom = minY; bottom <= highest; bottom += SectionPos.SECTION_SIZE) {
            section.read(buffer);
            readTops(heights, tops, belows, bottom);
        }

        surfaces.put(ChunkPos.pack(chunk.x(), chunk.z()), new ChunkSurface(heights, tops, belows));
    }

    void forget(ChunkPos pos) {
        surfaces.remove(pos.pack());
    }

    void update(BlockPos pos, BlockState state) {
        ChunkSurface surface = surface(pos.getX(), pos.getZ());
        if (surface != null) {
            surface.update(column(pos.getX(), pos.getZ()), pos.getY() - minY, state);
        }
    }

    void update(ClientboundSectionBlocksUpdatePacket packet) {
        List<Pair<BlockPos, BlockState>> changes = new ArrayList<>();
        packet.runUpdates((pos, state) -> changes.add(Pair.of(pos.immutable(), state)));
        changes.sort(Comparator.comparingInt(change -> -change.getFirst().getY()));
        changes.forEach(change -> update(change.getFirst(), change.getSecond()));
    }

    void land(Vec3 feet) {
        for (int blockX = Mth.floor(feet.x - HALF_WIDTH); blockX < Mth.ceil(feet.x + HALF_WIDTH); blockX++) {
            for (int blockZ = Mth.floor(feet.z - HALF_WIDTH); blockZ < Mth.ceil(feet.z + HALF_WIDTH); blockZ++) {
                ChunkSurface surface = surface(blockX, blockZ);
                if (surface != null) {
                    surface.land(column(blockX, blockZ), Mth.floor(feet.y) - minY);
                }
            }
        }
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

    OptionalInt ground(double x, double z) {
        int ground = minY;
        for (int blockX = Mth.floor(x - HALF_WIDTH); blockX < Mth.ceil(x + HALF_WIDTH); blockX++) {
            for (int blockZ = Mth.floor(z - HALF_WIDTH); blockZ < Mth.ceil(z + HALF_WIDTH); blockZ++) {
                OptionalInt height = height(blockX, blockZ);
                if (height.isEmpty()) {
                    return OptionalInt.empty();
                }

                ground = Math.max(ground, height.getAsInt());
            }
        }

        return OptionalInt.of(ground);
    }

    private int highestTop(SimpleBitStorage heights) {
        int highest = 0;
        for (int column = 0; column < COLUMNS; column++) {
            highest = Math.max(highest, heights.get(column));
        }

        return highest + minY - 1;
    }

    private void readTops(SimpleBitStorage heights, char[] tops, char[] belows, int bottom) {
        for (int column = 0; column < COLUMNS; column++) {
            int y = heights.get(column) + minY - 1 - bottom;
            if (y >= 0 && y < SectionPos.SECTION_SIZE) {
                tops[column] = blockId(column, y);
            }

            if (y >= 1 && y <= SectionPos.SECTION_SIZE) {
                belows[column] = blockId(column, y - 1);
            }
        }
    }

    private char blockId(int column, int y) {
        return (char) Block.getId(section.getBlockState(column & SectionPos.SECTION_MASK, y, column >> SectionPos.SECTION_BITS));
    }

    private @Nullable ChunkSurface surface(int blockX, int blockZ) {
        return surfaces.get(ChunkPos.pack(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ)));
    }

    private static int column(int blockX, int blockZ) {
        return (blockX & SectionPos.SECTION_MASK) + (blockZ & SectionPos.SECTION_MASK) * SectionPos.SECTION_SIZE;
    }
}
