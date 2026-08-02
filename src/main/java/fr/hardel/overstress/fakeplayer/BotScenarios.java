package fr.hardel.overstress.fakeplayer;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.scenario.DimensionsScenario;
import fr.hardel.overstress.fakeplayer.scenario.ElytraScenario;
import fr.hardel.overstress.fakeplayer.scenario.FightScenario;
import fr.hardel.overstress.fakeplayer.scenario.IdleScenario;
import fr.hardel.overstress.fakeplayer.scenario.MineScenario;
import fr.hardel.overstress.fakeplayer.scenario.SpawnerScenario;
import fr.hardel.overstress.fakeplayer.scenario.WanderScenario;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;

/**
 * The scenario registry. It is a plain Fabric registry, not synced: scenarios are behavior that only
 * ever runs server-side, and the client is never told a bot is one. Another mod extends the harness
 * with {@code Registry.register(BotScenarios.REGISTRY, id, scenario)} from its own initializer, and
 * the command picks the addition up with no change here.
 */
public final class BotScenarios {
    public static final ResourceKey<Registry<BotScenario>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Overstress.MOD_ID, "bot_scenario"));
    public static final Registry<BotScenario> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();

    private BotScenarios() {
    }

    /** Registers the built-in scenarios, and by loading this class is also what creates the registry. */
    public static void bootstrap() {
        register("idle", new IdleScenario());
        register("wander", new WanderScenario());
        register("elytra", new ElytraScenario());
        register("mine", new MineScenario());
        register("fight", new FightScenario());
        register("spawner", new SpawnerScenario());
        register("dimensions", new DimensionsScenario());
    }

    /** Used when a spawn command names no scenario, so a fleet gets a mixed profile. */
    public static BotScenario random(RandomSource random) {
        return REGISTRY.getRandom(random).orElseThrow(() -> new IllegalStateException("No bot scenario is registered")).value();
    }

    private static void register(String path, BotScenario scenario) {
        Registry.register(REGISTRY, Identifier.fromNamespaceAndPath(Overstress.MOD_ID, path), scenario);
    }
}
