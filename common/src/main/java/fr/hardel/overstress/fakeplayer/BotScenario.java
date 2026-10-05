package fr.hardel.overstress.fakeplayer;

import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

@FunctionalInterface
public interface BotScenario {

    void steer(BotPilot pilot, BotState state, RandomSource random);

    /** Runs in the server tick of the bot, on the thread that ticks its player. */
    default void assist(ServerPlayer player, BotState state) {
    }

    default BotStanding standing() {
        return BotStanding.PLAYER;
    }
}
