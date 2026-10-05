package fr.hardel.overstress.fakeplayer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BotState {
    public final RandomSource random;
    public final double spawnX;
    public final double spawnZ;
    public volatile double heading;
    public int cooldown;
    public int deaths;
    public double detour;
    public volatile @Nullable Vec3 portal;
    public volatile @Nullable Vec3 home;
    public @Nullable Vec3 last;
    public volatile boolean ready;
    public volatile @Nullable ResourceKey<Level> dimension;
    public int quarry = -1;
    public int chase;
    public double closest;
    public int hits;
    public final AbandonedTargets abandoned = new AbandonedTargets();
    public volatile BotScenario delegate;
    public int delegateTicks;

    volatile BotScenario scenario;

    BotState(BotScenario scenario, double spawnX, double spawnZ, long seed) {
        this.scenario = scenario;
        this.spawnX = spawnX;
        this.spawnZ = spawnZ;
        this.random = RandomSource.create(seed);
        this.heading = random.nextDouble() * Math.PI * 2;
    }

    public BotScenario scenario() {
        return scenario;
    }

    public float yRot() {
        return (float) Math.toDegrees(heading) - 90;
    }
}
