package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotMovement;
import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Walks and breaks a block every 8 ticks: block updates, drops, lighting and neighbour propagation. */
public final class MineScenario implements BotScenario {

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        BotMovement.walk(player, state.heading, 0.15);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = 8;
        state.heading += (random.nextDouble() - 0.5) * 0.6;
        ServerLevel level = player.level();
        BlockPos eye = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(eye.offset(-3, -1, -3), eye.offset(3, 2, 3))) {
            if (!level.getBlockState(pos).isAir() && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                level.destroyBlock(pos.immutable(), true, player);
                return;
            }
        }
    }
}
