package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotMovement;
import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** A plain fly, not an elytra: an elytra trades height for speed and sinks into the first mountain. */
public final class FlyScenario implements BotScenario {
    private static final double SPEED = 1.8;
    private static final int CLEARANCE = 100;

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        BotMovement.move(player, state.heading, SPEED, CLEARANCE);
    }
}
