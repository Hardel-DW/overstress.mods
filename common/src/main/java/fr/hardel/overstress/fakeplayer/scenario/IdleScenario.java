package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.util.RandomSource;

public final class IdleScenario implements BotScenario {

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
    }
}
