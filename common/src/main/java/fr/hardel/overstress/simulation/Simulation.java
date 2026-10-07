package fr.hardel.overstress.simulation;

import fr.hardel.overstress.fakeplayer.BotScenario;

/** The bots stand on a ring, {@code perSpot} of them around each spot of the ring, within {@code clusterRadius} blocks of it. */
public record Simulation(int bots, int radius, int clusterPercent, int clusterRadius, BotScenario scenario, int durationTicks, int spawnIntervalTicks, boolean mobSpawning, int perSpot) {
    public Simulation(int bots, int radius, int clusterPercent, int clusterRadius, BotScenario scenario, int durationTicks, int spawnIntervalTicks, boolean mobSpawning) {
        this(bots, radius, clusterPercent, clusterRadius, scenario, durationTicks, spawnIntervalTicks, mobSpawning, 1);
    }
}
