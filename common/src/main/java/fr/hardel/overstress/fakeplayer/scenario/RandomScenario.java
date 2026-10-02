package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotScenarios;
import fr.hardel.overstress.fakeplayer.BotStanding;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.util.RandomSource;

public final class RandomScenario implements BotScenario {
    private static final int PERIOD_TICKS = 20 * 60;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        if (--state.delegateTicks <= 0) {
            boolean first = state.delegate == null;
            state.delegate = drawOther(random);
            state.delegateTicks = first ? 1 + random.nextInt(PERIOD_TICKS) : PERIOD_TICKS;
            state.cooldown = 0;
        }

        state.delegate.steer(pilot, state, random);
    }

    @Override
    public BotStanding standing() {
        return BotScenarios.DIMENSIONS.standing();
    }

    private BotScenario drawOther(RandomSource random) {
        BotScenario drawn = BotScenarios.random(random);
        while (drawn == this) {
            drawn = BotScenarios.random(random);
        }

        return drawn;
    }
}
