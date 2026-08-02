package fr.hardel.overstress.fakeplayer;

/** Per-bot scratchpad, read and written only from the thread ticking that bot's level. */
public final class BotState {
    public final double spawnX;
    public final double spawnZ;
    public double heading;
    public int cooldown;
    public int dimensionIndex;

    final BotScenario scenario;
    boolean initialized;

    BotState(BotScenario scenario, double spawnX, double spawnZ) {
        this.scenario = scenario;
        this.spawnX = spawnX;
        this.spawnZ = spawnZ;
    }
}
