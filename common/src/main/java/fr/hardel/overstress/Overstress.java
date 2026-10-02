package fr.hardel.overstress;

import fr.hardel.overstress.fakeplayer.BotScenarios;
import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import fr.hardel.overstress.simulation.SimulationRunner;
import fr.hardel.overstress.simulation.Simulations;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Overstress {
    public static final String MOD_ID = "overstress";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Overstress() {
    }

    public static void bootstrap() {
        BotScenarios.bootstrap();
        Simulations.bootstrap();
    }

    public static void onServerStarting(MinecraftServer server) {
        FakePlayerManager.open(server);
    }

    public static void onServerStopping(MinecraftServer server) {
        FakePlayerManager.close();
    }

    public static void onServerTick(MinecraftServer server) {
        SimulationRunner.tick(server);
        FakePlayerManager.tick(server);
    }
}
