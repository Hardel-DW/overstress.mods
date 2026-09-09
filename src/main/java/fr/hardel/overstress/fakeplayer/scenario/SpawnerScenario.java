package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class SpawnerScenario implements BotScenario {

    @Override
    public void tick(ServerPlayer player, BotState state, RandomSource random) {
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = 20;
        ServerLevel level = player.level();
        int nearby = level.getEntitiesOfClass(Mob.class, AABB.ofSize(player.position(), 96, 64, 96)).size();
        for (int spawned = 0; spawned < 10 && nearby + spawned < 300; spawned++) {
            BlockPos pos = player.blockPosition().offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8);
            EntityType.ZOMBIE.spawn(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos), EntitySpawnReason.COMMAND);
        }
    }
}
