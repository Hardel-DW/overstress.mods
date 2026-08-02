package fr.hardel.overstress.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** One load behavior, held in {@link BotScenarios#REGISTRY} so another mod can contribute its own. */
@FunctionalInterface
public interface BotScenario {

    void tick(ServerPlayer player, BotState state, RandomSource random);
}
