package fr.hardel.overstress.fakeplayer;

import net.minecraft.util.RandomSource;

public final class BotState {

    public final RandomSource random;

    public final double spawnX;
    public final double spawnZ;
    public double heading;
    public int cooldown;
    public int dimensionIndex;
    public BotScenario delegate;
    public int delegateTicks;

    volatile BotScenario scenario;
    boolean initialized;

    BotState(BotScenario scenario, double spawnX, double spawnZ, long seed) {
        this.scenario = scenario;
        this.spawnX = spawnX;
        this.spawnZ = spawnZ;
        this.random = RandomSource.create(seed);
    }
}
