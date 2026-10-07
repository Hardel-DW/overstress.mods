package fr.hardel.overstress.simulation;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.BotScenarios;
import com.mojang.serialization.Lifecycle;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class Simulations {
    private static final int TOUCHING = 32;
    private static final int NEIGHBOURING = 256;
    public static final ResourceKey<Registry<Simulation>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Overstress.MOD_ID, "simulation"));
    public static final Registry<Simulation> REGISTRY = new MappedRegistry<>(KEY, Lifecycle.stable());

    private Simulations() {
    }

    public static void bootstrap() {
        register("smoke", new Simulation(8, 300, 5, TOUCHING, BotScenarios.IDLE, SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("idle", new Simulation(50, 2000, 5, TOUCHING, BotScenarios.IDLE, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("roam", new Simulation(40, 1500, 5, TOUCHING, BotScenarios.ELYTRA, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("border", new Simulation(20, 2100, 0, TOUCHING, BotScenarios.WANDER, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("mobs", new Simulation(20, 800, 5, TOUCHING, BotScenarios.SPAWNER, 3 * SharedConstants.TICKS_PER_MINUTE, 0, true));
        register("churn", new Simulation(60, 5000, 5, NEIGHBOURING, BotScenarios.CHURN, 10 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        // One idle bot every second on a ring wide enough for a region each: how many regions the machine holds at 20 TPS.
        register("ramp", new Simulation(1000, 70000, 0, NEIGHBOURING, BotScenarios.IDLE, 25 * SharedConstants.TICKS_PER_MINUTE, 20, false));
        // The same thousand idle bots in fifty groups of twenty: fifty regions, each shared by twenty players.
        register("hubs", new Simulation(1000, 70000, 0, TOUCHING, BotScenarios.IDLE, 25 * SharedConstants.TICKS_PER_MINUTE, 20, false, 20));
        register("sprawl", new Simulation(100, 50000, 5, NEIGHBOURING, BotScenarios.ELYTRA, 10 * SharedConstants.TICKS_PER_MINUTE, 60, false));
        register("solo", new Simulation(1, 0, 0, TOUCHING, BotScenarios.ELYTRA, 5 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("worldgen", new Simulation(5, 2000, 0, TOUCHING, BotScenarios.ELYTRA, 3 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("spread", new Simulation(20, 3000, 0, TOUCHING, BotScenarios.IDLE, 4 * SharedConstants.TICKS_PER_MINUTE, 0, false));
        register("pvp", new Simulation(16, 0, 100, TOUCHING, BotScenarios.PVP, 3 * SharedConstants.TICKS_PER_MINUTE, 0, true));
    }

    private static void register(String path, Simulation simulation) {
        Registry.register(REGISTRY, Identifier.fromNamespaceAndPath(Overstress.MOD_ID, path), simulation);
    }
}
