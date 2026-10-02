package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class FightScenario implements BotScenario {
    private static final double SPEED = 0.3;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        pilot.walk(state.heading, SPEED);
    }

    @Override
    public void act(ServerPlayer player, BotState state) {
        if (--state.actCooldown > 0) {
            return;
        }

        state.actCooldown = 10;
        Mob target = player.level().getEntitiesOfClass(Mob.class, AABB.ofSize(player.position(), 16, 8, 16)).stream().filter(Mob::isAlive).findFirst().orElse(null);
        if (target == null) {
            state.heading = player.getRandom().nextDouble() * Math.PI * 2;
            return;
        }

        state.heading = Math.atan2(target.getZ() - player.getZ(), target.getX() - player.getX());
        if (player.distanceTo(target) <= 3) {
            player.attack(target);
        }
    }
}
