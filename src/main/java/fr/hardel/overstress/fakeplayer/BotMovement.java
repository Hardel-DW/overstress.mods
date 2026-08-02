package fr.hardel.overstress.fakeplayer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/** Ground movement, shared by every scenario that walks. */
public final class BotMovement {

    private BotMovement() {
    }

    /**
     * One step of {@code speed} blocks along {@code heading}, snapped to the surface. Does nothing
     * while the target chunk is not loaded yet, which is what keeps a bot from outrunning worldgen.
     */
    public static void walk(ServerPlayer player, double heading, double speed) {
        double targetX = player.getX() + Math.cos(heading) * speed;
        double targetZ = player.getZ() + Math.sin(heading) * speed;
        ServerLevel level = player.level();
        if (level.getChunkSource().getChunkNow((int) targetX >> 4, (int) targetZ >> 4) == null) {
            return;
        }

        double floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(targetX), (int) Math.floor(targetZ));
        player.snapTo(targetX, floor, targetZ, (float) Math.toDegrees(heading) - 90, 0);
    }
}
