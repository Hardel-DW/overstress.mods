package fr.hardel.overstress.fakeplayer;

import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

@FunctionalInterface
public interface BotScenario {

    void steer(BotPilot pilot, BotState state, RandomSource random);

    default void act(ServerPlayer player, BotState state) {
    }
}
