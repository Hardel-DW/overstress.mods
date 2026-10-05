package fr.hardel.overstress.fakeplayer;

import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

@FunctionalInterface
public interface BotScenario {

    void steer(BotPilot pilot, BotState state, RandomSource random);

    default void prepare(ServerLevel level, BotState state) {
    }

    default void assist(ServerPlayer player, BotState state) {
    }

    default BotStanding standing() {
        return BotStanding.PLAYER;
    }
}
