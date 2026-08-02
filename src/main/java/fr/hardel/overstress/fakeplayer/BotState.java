package fr.hardel.overstress.fakeplayer;

/** Per-bot scratchpad owned by the ticking thread; {@code scenario} is volatile because a command reassigns it. */
public final class BotState {
    public final double spawnX;
    public final double spawnZ;
    public double heading;
    public int cooldown;
    public int dimensionIndex;
    public BotScenario delegate;
    public int delegateTicks;

    volatile BotScenario scenario;
    boolean initialized;

    BotState(BotScenario scenario, double spawnX, double spawnZ) {
        this.scenario = scenario;
        this.spawnX = spawnX;
        this.spawnZ = spawnZ;
    }
}
