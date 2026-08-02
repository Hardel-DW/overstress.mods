package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotMovement;
import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** Hunts the nearest mob within 16 blocks: entity queries, pathing pressure and damage handling. */
public final class FightScenario implements BotScenario {

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = 10;
        Mob target = player.level().getEntitiesOfClass(Mob.class, AABB.ofSize(player.position(), 16, 8, 16)).stream().filter(Mob::isAlive).findFirst().orElse(null);
        if (target == null) {
            BotMovement.walk(player, random.nextDouble() * Math.PI * 2, 0.4);
            return;
        }

        if (player.distanceTo(target) > 3) {
            BotMovement.walk(player, Math.atan2(target.getZ() - player.getZ(), target.getX() - player.getX()), 0.3);
        } else {
            player.attack(target);
        }
    }
}
