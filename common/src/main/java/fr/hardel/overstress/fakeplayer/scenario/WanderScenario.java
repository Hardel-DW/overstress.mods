package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.util.RandomSource;

public final class WanderScenario implements BotScenario {
    private static final double DIAGONAL = Math.PI / 4;
    private static final double SPEED = 0.2;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        pilot.walk(DIAGONAL, SPEED);
    }
}
