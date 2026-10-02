package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

final class ChunkSurface {
    private static final int UNKNOWN = 0;
    private final char[] tops;
    private SimpleBitStorage heights;
    private boolean ownsHeights;

    ChunkSurface(SimpleBitStorage heights, char[] tops) {
        this.heights = heights;
        this.tops = tops;
    }

    int height(int column) {
        return heights.get(column);
    }

    @Nullable BlockState top(int column) {
        BlockState state = Block.stateById(tops[column]);
        return state.isAir() ? null : state;
    }

    void update(int column, int height, BlockState state) {
        int current = heights.get(column);
        boolean blocks = Heightmap.Types.MOTION_BLOCKING.isOpaque().test(state);
        if (blocks && height >= current - 1) {
            tops[column] = (char) Block.getId(state);
            setHeight(column, Math.max(current, height + 1));
            return;
        }

        if (!blocks && height == current - 1) {
            tops[column] = 0;
            setHeight(column, UNKNOWN);
        }
    }

    void land(int column, int height) {
        if (heights.get(column) == UNKNOWN) {
            setHeight(column, height);
        }
    }

    private void setHeight(int column, int height) {
        if (heights.get(column) == height) {
            return;
        }

        if (!ownsHeights) {
            heights = new SimpleBitStorage(heights.getBits(), heights.getSize(), heights.getRaw().clone());
            ownsHeights = true;
        }

        heights.set(column, height);
    }
}
