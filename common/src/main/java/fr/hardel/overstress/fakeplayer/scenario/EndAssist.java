package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class EndAssist {
    private static final int PORTAL_OFFSET = 4;
    private static final BlockPos EXIT_CENTER = new BlockPos(0, 65, 0);
    private static final int EXIT_RADIUS = 3;
    private static final int EXIT_HEIGHT = 35;
    private static final int GATEWAYS = 20;
    private static final int GATEWAY_RADIUS = 96;
    private static final int GATEWAY_Y = 75;
    private static final int EXIT_GATEWAY_SEARCH = 12;
    private static final double MAIN_ISLAND = 200;

    private EndAssist() {
    }

    /** The end portal block of the bot, set next to it the first time it is needed. */
    static BlockPos home(ServerLevel level, ServerPlayer player, BotState state) {
        BlockPos home = state.home;
        if (home == null) {
            BlockPos beside = player.blockPosition().east(PORTAL_OFFSET);
            home = new BlockPos(beside.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, beside.getX(), beside.getZ()), beside.getZ());
            level.setBlockAndUpdate(home, Blocks.END_PORTAL.defaultBlockState());
            state.home = home;
        }

        return home;
    }

    static void killDragons(ServerLevel level) {
        for (EnderDragon dragon : level.getDragons()) {
            if (!dragon.isDeadOrDying()) {
                dragon.kill(level);
            }
        }
    }

    static @Nullable BlockPos exitPortal(ServerLevel level, ServerPlayer player) {
        return Portals.find(level, EXIT_CENTER, EXIT_RADIUS, EXIT_HEIGHT, Blocks.END_PORTAL);
    }

    static @Nullable BlockPos gateway(ServerLevel level, ServerPlayer player) {
        Vec3 from = player.position();
        return from.horizontalDistanceSqr() < MAIN_ISLAND * MAIN_ISLAND
            ? ringGateway(level, from)
            : Portals.find(level, player.blockPosition(), EXIT_GATEWAY_SEARCH, EXIT_GATEWAY_SEARCH, Blocks.END_GATEWAY);
    }

    private static @Nullable BlockPos ringGateway(ServerLevel level, Vec3 from) {
        BlockPos nearest = null;
        for (int index = 0; index < GATEWAYS; index++) {
            double angle = 2.0 * (-Math.PI + Math.PI / GATEWAYS * index);
            BlockPos pos = new BlockPos(Mth.floor(GATEWAY_RADIUS * Math.cos(angle)), GATEWAY_Y, Mth.floor(GATEWAY_RADIUS * Math.sin(angle)));
            if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.END_GATEWAY) && (nearest == null || pos.distToCenterSqr(from) < nearest.distToCenterSqr(from))) {
                nearest = pos;
            }
        }

        return nearest;
    }
}
