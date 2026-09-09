package fr.hardel.overstress.fakeplayer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/** Bots arriving one batch per interval, like real players trickling in; an interval of zero lands them all at once. */
final class ArrivalWave {
    private final Vec3 center;
    private final int spread;
    private final ClusterSpread cluster;
    private final @Nullable BotScenario forced;
    private final int intervalTicks;
    private final RandomSource random;
    private int remaining;
    private long nextTick;

    ArrivalWave(Vec3 center, int count, int spread, ClusterSpread cluster, @Nullable BotScenario forced, int intervalTicks, RandomSource random, long now) {
        this.center = center;
        this.spread = spread;
        this.cluster = cluster;
        this.forced = forced;
        this.intervalTicks = intervalTicks;
        this.random = random;
        this.remaining = count;
        this.nextTick = now;
    }

    /** True once the last bot landed. */
    boolean spawnDue(MinecraftServer server, long now) {
        while (remaining > 0 && now >= nextTick) {
            double x = center.x() + random.nextInt(spread * 2 + 1) - spread + 0.5;
            double z = center.z() + random.nextInt(spread * 2 + 1) - spread + 0.5;
            FakePlayerManager.spawn(server, FakePlayerManager.nextName(), cluster.next(new Vec3(x, 0, z)), forced != null ? forced : BotScenarios.random(random), random.nextLong());
            remaining--;
            nextTick = now + intervalTicks;
        }

        return remaining == 0;
    }
}
