package fr.hardel.overstress.fakeplayer.scenario;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

final class Portals {

    private Portals() {
    }

    /** Only loaded chunks are read: a search never loads a chunk on the calling thread. */
    static @Nullable BlockPos find(ServerLevel level, BlockPos center, int horizontal, int vertical, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-horizontal, -vertical, -horizontal), center.offset(horizontal, vertical, horizontal))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(block)) {
                return pos.immutable();
            }
        }

        return null;
    }

    static void enter(ServerPlayer player, BlockPos block) {
        player.teleportTo(block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
    }
}
