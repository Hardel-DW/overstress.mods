package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;

public final class NetherPortalScenario implements BotScenario {
    private static final int PORTAL_OFFSET = 4;
    private static final int INSIDE_WIDTH = 2;
    private static final int INSIDE_HEIGHT = 3;
    private static final int NEAR = 3;
    private static final int STAY_TICKS = 200;
    private static final int OUTSIDE_TICKS = 20;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
    }

    /** A portal only takes a player who left it: the bot steps out for a second, then back in. */
    @Override
    public void assist(ServerPlayer player, BotState state) {
        ServerLevel level = player.level();
        if (state.arrives(level.dimension())) {
            state.trip = STAY_TICKS;
            state.outside = false;
            return;
        }

        if (--state.trip > 0) {
            return;
        }

        BlockPos near = Portals.find(level, player.blockPosition(), NEAR, NEAR, Blocks.NETHER_PORTAL);
        BlockPos portal = near != null ? near : build(level, player.blockPosition().east(PORTAL_OFFSET));
        Direction beside = level.getBlockState(portal).getValue(NetherPortalBlock.AXIS) == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        state.outside = !state.outside;
        state.trip = state.outside ? OUTSIDE_TICKS : STAY_TICKS;
        Portals.enter(player, state.outside ? portal.relative(beside) : portal);
    }

    private static BlockPos build(ServerLevel level, BlockPos origin) {
        int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        for (int dx = -1; dx <= INSIDE_WIDTH; dx++) {
            for (int dy = 0; dy <= INSIDE_HEIGHT + 1; dy++) {
                boolean frame = dx == -1 || dx == INSIDE_WIDTH || dy == 0 || dy == INSIDE_HEIGHT + 1;
                level.setBlockAndUpdate(new BlockPos(origin.getX() + dx, floor + dy, origin.getZ()), frame ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }

        BlockPos bottom = new BlockPos(origin.getX(), floor + 1, origin.getZ());
        PortalShape.findEmptyPortalShape(level, bottom, Direction.Axis.X).ifPresent(shape -> shape.createPortalBlocks(level));
        return bottom;
    }
}
