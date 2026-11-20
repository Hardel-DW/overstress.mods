package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import fr.hardel.overstress.fakeplayer.client.SeenEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

/** A cheater to test a detection against: it aims as a player does, and hits from further than a player can, under the distance at which the server drops the hit. */
public final class ReachScenario implements BotScenario {
    private static final double REACH = 5.5;
    private static final double SPEED = 0.3;
    private static final int TURN_TICKS = 40;
    private static final double SIGHT_WIDTH = 32;
    private static final double SIGHT_HEIGHT = 16;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        Melee.wield(pilot);
        SeenEntity foe = pilot.entities().nearest(pilot.position(), AABB.ofSize(pilot.position(), SIGHT_WIDTH, SIGHT_HEIGHT, SIGHT_WIDTH),
            entity -> pilot.entities().hurtable(entity) && Melee.pursuable(pilot, state, entity));
        if (foe != null) {
            Melee.engage(pilot, state, foe, SPEED, REACH, foe.box().getCenter());
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
