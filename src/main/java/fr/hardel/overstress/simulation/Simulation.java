package fr.hardel.overstress.simulation;

import fr.hardel.overstress.fakeplayer.BotScenario;

public record Simulation(int bots, int radius, int clusterPercent, BotScenario scenario, int durationTicks, boolean mobSpawning) {
}
