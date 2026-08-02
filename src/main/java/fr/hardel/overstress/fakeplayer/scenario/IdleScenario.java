package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Stands still and does nothing. This is the baseline: what a connected player costs before it moves,
 * which is chunk tickets, entity tracking and its share of the player list, and nothing else. Subtract
 * it from any other scenario to get that scenario's own cost.
 */
public final class IdleScenario implements BotScenario {

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
    }
}
