package fr.hardel.overstress.simulation;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.BotScenarios;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class Simulations {
    private static final int TOUCHING = 32;
    private static final int NEIGHBOURING = 256;
    public static final ResourceKey<Registry<Simulation>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Overstress.MOD_ID, "simulation"));
    public static final Registry<Simulation> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();

    private Simulations() {
    }

    public static void bootstrap() {
        register("smoke", new Simulation(8, 300, 5, TOUCHING, BotScenarios.IDLE, SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("idle", new Simulation(50, 2000, 5, TOUCHING, BotScenarios.IDLE, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("roam", new Simulation(40, 1500, 5, TOUCHING, BotScenarios.FLY, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("border", new Simulation(20, 2100, 0, TOUCHING, BotScenarios.WANDER, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("mobs", new Simulation(20, 800, 5, TOUCHING, BotScenarios.SPAWNER, 3 * SharedConstants.TICKS_PER_MINUTE, 0, true));
        register("churn", new Simulation(60, 5000, 5, NEIGHBOURING, BotScenarios.CHURN, 10 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("ramp", new Simulation(300, 20000, 10, NEIGHBOURING, BotScenarios.MINE, 15 * SharedConstants.TICKS_PER_MINUTE, 60, false));
        register("sprawl", new Simulation(100, 50000, 5, NEIGHBOURING, BotScenarios.FLY, 10 * SharedConstants.TICKS_PER_MINUTE, 60, false));
        register("flight", new Simulation(1, 0, 0, TOUCHING, BotScenarios.FLY, 5 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("spread", new Simulation(20, 3000, 0, TOUCHING, BotScenarios.IDLE, 4 * SharedConstants.TICKS_PER_MINUTE, 0, false));
    }

    private static void register(String path, Simulation simulation) {
        Registry.register(REGISTRY, Identifier.fromNamespaceAndPath(Overstress.MOD_ID, path), simulation);
    }
}
