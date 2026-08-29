package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotMovement;
import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public final class ChurnScenario implements BotScenario {
    private static final double AMPLITUDE = 450;
    private static final int HALF_PERIOD_TICKS = 300;

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        double spawnDistance = Math.sqrt(state.spawnX * state.spawnX + state.spawnZ * state.spawnZ);
        double step = distanceAt(player.level().getServer().getTickCount(), spawnDistance) - distanceFromCentre(player);
        double outward = Math.atan2(state.spawnZ, state.spawnX);
        BotMovement.walk(player, step >= 0 ? outward : outward + Math.PI, Math.min(Math.abs(step), speed()));
    }

    private static double distanceAt(long tick, double spawnDistance) {
        long phase = Math.floorMod(tick, 2L * HALF_PERIOD_TICKS);
        double closed = phase < HALF_PERIOD_TICKS ? phase : 2L * HALF_PERIOD_TICKS - phase;

        return spawnDistance - AMPLITUDE * closed / HALF_PERIOD_TICKS;
    }

    private static double distanceFromCentre(ServerPlayer player) {
        return Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ());
    }

    private static double speed() {
        return AMPLITUDE / HALF_PERIOD_TICKS;
    }
}
