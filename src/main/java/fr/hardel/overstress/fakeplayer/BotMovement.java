package fr.hardel.overstress.fakeplayer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BotMovement {

    private BotMovement() {
    }

    public static void walk(ServerPlayer player, double heading, double speed) {
        move(player, heading, speed, 0);
    }

    public static void move(ServerPlayer player, double heading, double speed, int clearance) {
        double targetX = player.getX() + Math.cos(heading) * speed;
        double targetZ = player.getZ() + Math.sin(heading) * speed;
        ServerLevel level = player.level();
        int blockX = (int) Math.floor(targetX);
        int blockZ = (int) Math.floor(targetZ);
        if (level.getChunkSource().getChunkNow(blockX >> 4, blockZ >> 4) == null) {
            return;
        }

        double floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
        place(player, targetX, floor + clearance, targetZ, (float) Math.toDegrees(heading) - 90);
    }

    public static void place(ServerPlayer player, double x, double y, double z, float yRot) {
        player.snapTo(x, y, z, yRot, 0);
        player.level().getChunkSource().move(player);
    }
}
