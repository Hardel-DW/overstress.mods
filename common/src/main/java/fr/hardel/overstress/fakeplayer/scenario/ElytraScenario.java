package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.util.RandomSource;

public final class ElytraScenario implements BotScenario {
    static final double SPEED = 1.8;
    static final int ALTITUDE = 350;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        pilot.fly(state.heading, SPEED, ALTITUDE);
    }
}
