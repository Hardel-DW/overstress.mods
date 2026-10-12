package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.function.Predicate;

final class ChunkSurface {
    private static final Predicate<BlockState> MOTION_BLOCKING = Heightmap.Types.MOTION_BLOCKING.isOpaque();
    private final char[] tops;
    private final char[] belows;
    private final BitSet unknown = new BitSet();
    private SimpleBitStorage heights;
    private boolean ownsHeights;

    ChunkSurface(SimpleBitStorage heights, char[] tops, char[] belows) {
        this.heights = heights;
        this.tops = tops;
        this.belows = belows;
    }

    int height(int column) {
        return heights.get(column);
    }

    boolean known(int column) {
        return !unknown.get(column);
    }

    @Nullable BlockState top(int column) {
        BlockState state = Block.stateById(tops[column]);
        return state.isAir() ? null : state;
    }

    void update(int column, int height, BlockState state) {
        int current = heights.get(column);
        boolean blocks = MOTION_BLOCKING.test(state);
        if (blocks && height >= current - 1) {
            unknown.clear(column);
            belows[column] = height == current ? tops[column] : 0;
            tops[column] = (char) Block.getId(state);
            setHeight(column, Math.max(current, height + 1));
            return;
        }

        if (height == current - 2) {
            belows[column] = blocks ? (char) Block.getId(state) : 0;
            return;
        }

        if (!blocks && height == current - 1) {
            boolean grounded = MOTION_BLOCKING.test(Block.stateById(belows[column]));
            tops[column] = grounded ? belows[column] : 0;
            belows[column] = 0;
            unknown.set(column, !grounded);
            setHeight(column, height);
        }
    }

    void land(int column, int height) {
        if (unknown.get(column)) {
            unknown.clear(column);
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
