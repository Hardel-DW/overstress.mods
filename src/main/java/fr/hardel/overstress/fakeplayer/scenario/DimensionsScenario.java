package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotMovement;
import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Wanders, then crosses to the next dimension every 15 seconds: the cross-level transfer path. */
public final class DimensionsScenario implements BotScenario {

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        BotMovement.walk(player, state.heading, 0.2);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = 300;
        state.heading = random.nextDouble() * Math.PI * 2;
        List<ServerLevel> levels = new ArrayList<>();
        player.level().getServer().getAllLevels().forEach(levels::add);
        ServerLevel target = levels.get(++state.dimensionIndex % levels.size());
        if (target == player.level()) {
            return;
        }

        player.teleport(new TeleportTransition(target, new Vec3(state.spawnX, 128, state.spawnZ), Vec3.ZERO, player.getYRot(), 0, TeleportTransition.DO_NOTHING));
    }
}
