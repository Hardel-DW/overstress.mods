package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public final class MineScenario implements BotScenario {
    private static final int PERIOD_TICKS = 8;
    private static final double SPEED = 0.15;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        pilot.walk(state.heading, SPEED);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = PERIOD_TICKS;
        state.heading += (random.nextDouble() - 0.5) * 0.6;
    }

    @Override
    public void act(ServerPlayer player, BotState state) {
        if (--state.actCooldown > 0) {
            return;
        }

        state.actCooldown = PERIOD_TICKS;
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
