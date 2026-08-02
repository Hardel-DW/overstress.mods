package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Does nothing: the baseline of what a connected player costs before it moves. */
public final class IdleScenario implements BotScenario {

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
    }
}
