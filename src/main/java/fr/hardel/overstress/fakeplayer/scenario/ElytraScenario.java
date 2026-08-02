package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** Cruises 40 blocks above the surface at flight speed: the fastest way to force chunk generation. */
public final class ElytraScenario implements BotScenario {

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        if (random.nextInt(200) == 0) {
            state.heading = random.nextDouble() * Math.PI * 2;
        }
        double targetX = player.getX() + Math.cos(state.heading) * 1.8;
        double targetZ = player.getZ() + Math.sin(state.heading) * 1.8;
        ServerLevel level = player.level();
        if (level.getChunkSource().getChunkNow((int) targetX >> 4, (int) targetZ >> 4) == null) {
            return;
        }

        double floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) targetX, (int) targetZ);
        player.snapTo(targetX, Math.max(floor + 40, player.getY() - 0.5), targetZ, (float) Math.toDegrees(state.heading) - 90, 0);
    }
}
