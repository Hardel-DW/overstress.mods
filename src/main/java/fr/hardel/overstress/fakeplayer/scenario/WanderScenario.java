package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotMovement;
import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/**
 * One fixed diagonal, never turning: the diagonal crosses a chunk border on both axes at once, and
 * keeping the heading keeps bots equidistant so the region layout under test stays stable.
 */
public final class WanderScenario implements BotScenario {
    private static final double DIAGONAL = Math.PI / 4;
    private static final double SPEED = 0.2;

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        BotMovement.walk(player, DIAGONAL, SPEED);
    }
}
