package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import fr.hardel.overstress.fakeplayer.client.SeenEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;

public final class FightScenario implements BotScenario {
    private static final double SPEED = 0.3;
    private static final int TURN_TICKS = 10;
    private static final double SIGHT_WIDTH = 16;
    private static final double SIGHT_HEIGHT = 8;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        Melee.wield(pilot);
        SeenEntity target = pilot.entities().nearest(pilot.position(), AABB.ofSize(pilot.position(), SIGHT_WIDTH, SIGHT_HEIGHT, SIGHT_WIDTH),
            entity -> entity.type().getCategory() != MobCategory.MISC);
        if (target != null) {
            Melee.engage(pilot, target, SPEED);
            return;
        }

        pilot.walk(state.heading, SPEED);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = TURN_TICKS;
        state.heading = random.nextDouble() * Math.PI * 2;
    }
}
