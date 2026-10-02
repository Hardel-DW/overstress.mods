package fr.hardel.overstress.fakeplayer;

import net.minecraft.util.RandomSource;

public final class BotState {
    public final RandomSource random;
    public final double spawnX;
    public final double spawnZ;
    public volatile double heading;
    public int cooldown;
    public int deaths;
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
