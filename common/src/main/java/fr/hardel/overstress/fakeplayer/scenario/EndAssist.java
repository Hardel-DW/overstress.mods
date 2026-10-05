package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class EndAssist {
    private static final int PORTAL_OFFSET = 4;
    private static final int PORTAL_SIZE = 3;
    private static final double HOME_RADIUS = 64;
    private static final int EXIT_RADIUS = 3;
    private static final int EXIT_MIN_Y = 30;
    private static final int EXIT_MAX_Y = 100;
    private static final int GATEWAYS = 20;
    private static final int GATEWAY_RADIUS = 96;
    private static final int GATEWAY_Y = 75;
    private static final int EXIT_GATEWAY_SEARCH = 12;

    private EndAssist() {
    }

    static void buildPortal(ServerLevel level, BotState state) {
        int x = Mth.floor(state.spawnX) + PORTAL_OFFSET;
        int z = Mth.floor(state.spawnZ);
        int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        for (int dx = 0; dx < PORTAL_SIZE; dx++) {
            for (int dz = 0; dz < PORTAL_SIZE; dz++) {
                level.setBlockAndUpdate(new BlockPos(x + dx, floor, z + dz), Blocks.END_PORTAL.defaultBlockState());
            }
        }

        state.home = Vec3.atBottomCenterOf(new BlockPos(x + 1, floor, z + 1));
    }

    static void killDragons(ServerLevel level) {
        for (EnderDragon dragon : level.getDragons()) {
            if (!dragon.isDeadOrDying()) {
                dragon.kill(level);
            }
        }
    }

    static void bringHome(ServerPlayer player, BotState state) {
        Vec3 home = state.home;
        if (player.level().dimension() == Level.OVERWORLD && home != null && player.position().distanceToSqr(home) > HOME_RADIUS * HOME_RADIUS) {
            player.teleportTo(home.x - PORTAL_OFFSET, home.y, home.z);
        }
    }

    static @Nullable BlockPos exitPortal(ServerLevel level) {
        for (int y = EXIT_MIN_Y; y <= EXIT_MAX_Y; y++) {
            BlockPos found = around(level, new BlockPos(0, y, 0), EXIT_RADIUS, 0, Blocks.END_PORTAL);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    static @Nullable BlockPos ringGateway(ServerLevel level, Vec3 from) {
        BlockPos nearest = null;
        for (int index = 0; index < GATEWAYS; index++) {
            double angle = 2.0 * (-Math.PI + Math.PI / GATEWAYS * index);
            BlockPos pos = new BlockPos(Mth.floor(GATEWAY_RADIUS * Math.cos(angle)), GATEWAY_Y, Mth.floor(GATEWAY_RADIUS * Math.sin(angle)));
            if (level.getBlockState(pos).is(Blocks.END_GATEWAY) && (nearest == null || pos.distToCenterSqr(from) < nearest.distToCenterSqr(from))) {
                nearest = pos;
            }
        }

        return nearest;
    }

    static @Nullable BlockPos gatewayNear(ServerLevel level, Vec3 center) {
        return around(level, BlockPos.containing(center), EXIT_GATEWAY_SEARCH, EXIT_GATEWAY_SEARCH, Blocks.END_GATEWAY);
    }

    static void enter(ServerPlayer player, BlockPos block) {
        player.teleportTo(block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
    }

    private static @Nullable BlockPos around(ServerLevel level, BlockPos center, int horizontal, int vertical, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-horizontal, -vertical, -horizontal), center.offset(horizontal, vertical, horizontal))) {
            if (level.getBlockState(pos).is(block)) {
                return pos.immutable();
            }
        }

        return null;
    }
}
