package fr.hardel.overstress.simulation;

import fr.hardel.overstress.fakeplayer.BotScenario;

public record Simulation(int bots, int radius, int clusterPercent, int clusterRadius, BotScenario scenario, int durationTicks, int spawnIntervalTicks, boolean mobSpawning) {
}
