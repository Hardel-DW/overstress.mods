package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotMovement;
import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * Walks one fixed diagonal and never turns. The diagonal is the point: it crosses a chunk border on
 * both axes at once, so it pulls in new chunks about twice as fast as an axis-aligned line for the
 * same speed. Never turning is also the point, because bots that keep their heading keep their
 * distance from each other, and the region layout under test stays stable instead of churning.
 */
public final class WanderScenario implements BotScenario {
    private static final double DIAGONAL = Math.PI / 4;
    private static final double SPEED = 0.2;

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        BotMovement.walk(player, DIAGONAL, SPEED);
    }
}
