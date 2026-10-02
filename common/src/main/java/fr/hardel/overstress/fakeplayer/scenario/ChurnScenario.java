package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.util.RandomSource;

public final class ChurnScenario implements BotScenario {
    private static final double AMPLITUDE = 450;
    private static final int HALF_PERIOD_TICKS = 300;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        double spawnDistance = Math.sqrt(state.spawnX * state.spawnX + state.spawnZ * state.spawnZ);
        double step = distanceAt(pilot.ticks(), spawnDistance) - Math.sqrt(pilot.x() * pilot.x() + pilot.z() * pilot.z());
        double outward = Math.atan2(state.spawnZ, state.spawnX);
        pilot.walk(step >= 0 ? outward : outward + Math.PI, Math.min(Math.abs(step), speed()));
    }

    private static double distanceAt(long tick, double spawnDistance) {
        long phase = Math.floorMod(tick, 2L * HALF_PERIOD_TICKS);
        double closed = phase < HALF_PERIOD_TICKS ? phase : 2L * HALF_PERIOD_TICKS - phase;

        return spawnDistance - AMPLITUDE * closed / HALF_PERIOD_TICKS;
    }

    private static double speed() {
        return AMPLITUDE / HALF_PERIOD_TICKS;
    }
}
