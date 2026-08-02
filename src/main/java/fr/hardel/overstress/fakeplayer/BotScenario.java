package fr.hardel.overstress.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * One load behavior a bot can run. Implementations live in {@link BotScenarios#REGISTRY}, so another
 * mod can contribute its own profile from its initializer without touching this one.
 */
@FunctionalInterface
public interface BotScenario {

    /**
     * Runs once per tick of the level owning {@code player}, on the thread ticking that level.
     *
     * @param state scratchpad private to this bot, reached from no other thread
     */
    void tick(ServerPlayer player, BotState state, RandomSource random);
}
