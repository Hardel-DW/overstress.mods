package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotScenarios;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Runs another scenario for a minute, then draws a new one. Each bot draws on its own and its first
 * period is cut short at random, so a fleet spawned in one tick drifts apart instead of switching together.
 */
public final class RandomScenario implements BotScenario {
    private static final int PERIOD_TICKS = 20 * 60;

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        if (--state.delegateTicks <= 0) {
            boolean first = state.delegate == null;
            state.delegate = drawOther(random);
            state.delegateTicks = first ? 1 + random.nextInt(PERIOD_TICKS) : PERIOD_TICKS;
            state.cooldown = 0;
        }

        state.delegate.tick(player, state, random);
    }

    /** Anything but this scenario, which would recurse until the stack gives out. */
    private BotScenario drawOther(RandomSource random) {
        BotScenario drawn = BotScenarios.random(random);
        while (drawn == this) {
            drawn = BotScenarios.random(random);
        }

        return drawn;
    }
}
