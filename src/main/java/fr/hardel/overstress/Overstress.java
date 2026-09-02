package fr.hardel.overstress;

import fr.hardel.overstress.fakeplayer.BotScenarios;
import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import fr.hardel.overstress.simulation.SimulationRunner;
import fr.hardel.overstress.simulation.Simulations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Overstress implements ModInitializer {
    public static final String MOD_ID = "overstress";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        BotScenarios.bootstrap();
        Simulations.bootstrap();
        OverstressCommand.register();
        ServerTickEvents.END_SERVER_TICK.register(SimulationRunner::tick);
        ServerTickEvents.END_SERVER_TICK.register(FakePlayerManager::tick);
    }
}
