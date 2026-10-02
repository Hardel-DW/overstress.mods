package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotStanding;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import fr.hardel.overstress.fakeplayer.client.ClientStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DimensionsScenario implements BotScenario {
    private static final BotStanding OPERATOR = new BotStanding(GameType.SURVIVAL, true, true);
    private static final int PERIOD_TICKS = 300;
    private static final double SPEED = 0.2;
    private static final int ARRIVAL_Y = 128;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        pilot.walk(state.heading, SPEED);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = PERIOD_TICKS;
        state.heading = random.nextDouble() * Math.PI * 2;
        ClientStatus status = pilot.status();
        List<ResourceKey<Level>> levels = status.levels();
        ResourceKey<Level> next = levels.get((levels.indexOf(status.dimension()) + 1) % levels.size());
        pilot.hands().command("execute in %s run tp @s %d %d %d".formatted(next.identifier(), Mth.floor(state.spawnX), ARRIVAL_Y, Mth.floor(state.spawnZ)));
    }

    @Override
    public BotStanding standing() {
        return OPERATOR;
    }
}
