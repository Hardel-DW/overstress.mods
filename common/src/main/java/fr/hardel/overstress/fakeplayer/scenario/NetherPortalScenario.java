package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;

public final class NetherPortalScenario implements BotScenario {
    private static final int PORTAL_OFFSET = 4;
    private static final int INSIDE_WIDTH = 2;
    private static final int INSIDE_HEIGHT = 3;
    private static final int AWAY_TICKS = 100;
    private static final double SPEED = 0.2;
    private static final double INSIDE = 0.3;

    @Override
    public void prepare(ServerLevel level, BotState state) {
        int x = Mth.floor(state.spawnX) + PORTAL_OFFSET;
        int z = Mth.floor(state.spawnZ);
        int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int dx = -1; dx <= INSIDE_WIDTH; dx++) {
            for (int dy = 0; dy <= INSIDE_HEIGHT + 1; dy++) {
                boolean frame = dx == -1 || dx == INSIDE_WIDTH || dy == 0 || dy == INSIDE_HEIGHT + 1;
                level.setBlockAndUpdate(new BlockPos(x + dx, floor + dy, z), frame ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }

        BlockPos bottom = new BlockPos(x, floor + 1, z);
        PortalShape.findEmptyPortalShape(level, bottom, Direction.Axis.X).ifPresent(shape -> shape.createPortalBlocks(level));
        state.portal = Vec3.atBottomCenterOf(bottom);
    }

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        if (state.dimension != pilot.status().dimension()) {
            boolean arrived = state.dimension != null;
            state.dimension = pilot.status().dimension();
            state.portal = arrived ? pilot.position() : state.portal;
            state.cooldown = arrived ? AWAY_TICKS : 0;
        }

        if (state.portal == null) {
            return;
        }

        if (state.cooldown > 0) {
            state.cooldown--;
            pilot.walk(state.heading, SPEED);
            return;
        }

        if (pilot.position().distanceToSqr(state.portal.x, pilot.position().y, state.portal.z) > INSIDE * INSIDE) {
            pilot.walkTo(state.portal, SPEED);
        }
    }
}
