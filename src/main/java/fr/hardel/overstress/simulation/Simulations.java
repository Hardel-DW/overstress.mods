package fr.hardel.overstress.simulation;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.BotScenarios;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class Simulations {
    public static final ResourceKey<Registry<Simulation>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Overstress.MOD_ID, "simulation"));
    public static final Registry<Simulation> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();

    private Simulations() {
    }

    public static void bootstrap() {
        register("smoke", new Simulation(8, 300, 5, BotScenarios.IDLE, SharedConstants.TICKS_PER_MINUTE, false));
        register("idle", new Simulation(50, 2000, 5, BotScenarios.IDLE, 3 * SharedConstants.TICKS_PER_MINUTE, false));
        register("roam", new Simulation(40, 1500, 5, BotScenarios.FLY, 3 * SharedConstants.TICKS_PER_MINUTE, false));
        register("border", new Simulation(20, 2100, 0, BotScenarios.WANDER, 3 * SharedConstants.TICKS_PER_MINUTE, false));
        register("mobs", new Simulation(20, 800, 5, BotScenarios.SPAWNER, 3 * SharedConstants.TICKS_PER_MINUTE, true));
    }

    private static void register(String path, Simulation simulation) {
        Registry.register(REGISTRY, Identifier.fromNamespaceAndPath(Overstress.MOD_ID, path), simulation);
    }
}
