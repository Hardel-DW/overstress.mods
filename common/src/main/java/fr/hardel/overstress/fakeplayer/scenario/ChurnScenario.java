package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.util.RandomSource;

public final class ChurnScenario implements BotScenario {
    private static final double AMPLITUDE = 450;
    private static final long HALF_PERIOD_TICKS = Math.round(AMPLITUDE / ElytraScenario.SPEED);

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        boolean outward = Math.floorMod(pilot.ticks(), 2 * HALF_PERIOD_TICKS) < HALF_PERIOD_TICKS;
        pilot.fly(outward ? state.heading : state.heading + Math.PI, ElytraScenario.SPEED, ElytraScenario.ALTITUDE);
    }
}
