package fr.hardel.overstress.fakeplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class BotState {
    public final RandomSource random;
    public final double spawnX;
    public final double spawnZ;
    public volatile double heading;
    public int cooldown;
    public int deaths;
    public volatile @Nullable DigOrder order;
    public @Nullable DigOrder seen;
    public @Nullable DigOrder done;
    public int stall;
    public long shaved = Long.MAX_VALUE;
    public @Nullable BlockPos home;
    public @Nullable ResourceKey<Level> dimension;
    public boolean outside;
    public int trip;
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

    /** True the first time the server sees the bot in this dimension. */
    public boolean arrives(ResourceKey<Level> entered) {
        boolean arrives = dimension != entered;
        dimension = entered;
        return arrives;
    }
}
