package fr.hardel.overstress.fakeplayer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

@FunctionalInterface
public interface BotScenario {

    void tick(ServerPlayer player, BotState state, RandomSource random);
}
