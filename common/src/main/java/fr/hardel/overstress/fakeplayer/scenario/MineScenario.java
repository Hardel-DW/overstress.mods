package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.DigOrder;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class MineScenario implements BotScenario {
    private static final double SPEED = 0.15;
    private static final int STALL_TICKS = 100;
    private static final double ARRIVED = 1.0E-4;

    /** The bot obeys the order: it breaks the block with real packets, or walks to the foothold. An order it cannot finish makes it turn. */
    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        DigOrder order = state.order;
        if (order == null) {
            return;
        }

        if (order != state.seen) {
            state.seen = order;
            state.stall = 0;
        }

        BlockPos target = order.block();
        BlockState block = order.state();
        if (target != null && block != null && order != state.done) {
            pilot.hands().select(pilot.inventory().best(item -> item.getDestroySpeed(block)));
            pilot.look(Vec3.atCenterOf(target));
            if (pilot.hands().mine(target, Direction.UP, block)) {
                state.done = order;
            }

            return;
        }

        if (++state.stall > STALL_TICKS) {
            state.heading += Math.PI / 2;
            state.order = null;
            return;
        }

        pilot.stepTo(order.foot(), SPEED);
    }

    /** A new order only when the last one is over: its block is gone, or the bot stands on its foothold. */
    @Override
    public void assist(ServerPlayer player, BotState state) {
        ServerLevel level = player.level();
        DigOrder order = state.order;
        if (order == null) {
            if (player.onGround()) {
                BlockPos feet = player.blockPosition();
                state.order = new DigOrder(null, null, new Vec3(feet.getX() + 0.5, player.getY(), feet.getZ() + 0.5));
            }

            return;
        }

        BlockPos target = order.block();
        boolean busy = target != null ? level.getBlockState(target) == order.state() : player.position().distanceToSqr(order.foot()) > ARRIVED;
        if (!busy) {
            state.order = plan(level, player, state);
        }
    }

    /**
     * On the surface the bot takes the top block of the column ahead, once per column, then walks on.
     * Facing a wall it digs a staircase up: the block ahead at head height and the one above, then one step up.
     */
    private static @Nullable DigOrder plan(ServerLevel level, ServerPlayer player, BotState state) {
        BlockPos feet = player.blockPosition();
        BlockPos ahead = feet.relative(Direction.getApproximateNearest(Math.cos(state.heading), 0, Math.sin(state.heading)));
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, ahead.getX(), ahead.getZ());
        boolean open = surface <= feet.getY() + 1;
        BlockPos top = new BlockPos(ahead.getX(), surface - 1, ahead.getZ());
        long column = BlockPos.asLong(ahead.getX(), 0, ahead.getZ());
        if (open && surface >= feet.getY() && state.shaved != column && solid(level, top) && level.getBlockState(top).getDestroySpeed(level, top) >= 0) {
            state.shaved = column;
            return new DigOrder(top, level.getBlockState(top), player.position());
        }

        boolean climbs = open ? surface == feet.getY() + 1 : solid(level, ahead);
        BlockPos obstacle = climbs ? firstSolid(level, feet.above(2), ahead.above(), ahead.above(2)) : firstSolid(level, ahead, ahead.above());
        if (obstacle != null && level.getBlockState(obstacle).getDestroySpeed(level, obstacle) < 0) {
            state.heading += Math.PI / 2;
            return null;
        }

        if (obstacle != null) {
            return new DigOrder(obstacle, level.getBlockState(obstacle), player.position());
        }

        int landing = open ? surface : climbs ? feet.getY() + 1 : floor(level, ahead);
        return new DigOrder(null, null, new Vec3(ahead.getX() + 0.5, landing, ahead.getZ() + 0.5));
    }

    private static @Nullable BlockPos firstSolid(ServerLevel level, BlockPos... path) {
        for (BlockPos pos : path) {
            if (solid(level, pos)) {
                return pos;
            }
        }

        return null;
    }

    /** A liquid has no collision: the bot never digs it, and walks through it under the surface. */
    private static boolean solid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static int floor(ServerLevel level, BlockPos from) {
        BlockPos.MutableBlockPos probe = from.mutable().move(Direction.DOWN);
        while (probe.getY() >= level.getMinY() && !solid(level, probe)) {
            probe.move(Direction.DOWN);
        }

        return probe.getY() + 1;
    }
}
