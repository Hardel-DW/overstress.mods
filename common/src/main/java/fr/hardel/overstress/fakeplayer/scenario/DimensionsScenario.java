package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class DimensionsScenario implements BotScenario {
    private static final int PERIOD_TICKS = 300;
    private static final double SPEED = 0.2;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        pilot.walk(state.heading, SPEED);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = PERIOD_TICKS;
        state.heading = random.nextDouble() * Math.PI * 2;
    }

    @Override
    public void act(ServerPlayer player, BotState state) {
        if (--state.actCooldown > 0) {
            return;
        }

        state.actCooldown = PERIOD_TICKS;
        List<ServerLevel> levels = new ArrayList<>();
        player.level().getServer().getAllLevels().forEach(levels::add);
        ServerLevel target = levels.get(++state.dimensionIndex % levels.size());
        if (target == player.level()) {
            return;
        }

        player.teleport(new TeleportTransition(target, new Vec3(state.spawnX, 128, state.spawnZ), Vec3.ZERO, player.getYRot(), 0, TeleportTransition.DO_NOTHING));
    }
}
