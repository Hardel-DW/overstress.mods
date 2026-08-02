package fr.hardel.overstress.fakeplayer;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.scenario.DimensionsScenario;
import fr.hardel.overstress.fakeplayer.scenario.FightScenario;
import fr.hardel.overstress.fakeplayer.scenario.FlyScenario;
import fr.hardel.overstress.fakeplayer.scenario.IdleScenario;
import fr.hardel.overstress.fakeplayer.scenario.MineScenario;
import fr.hardel.overstress.fakeplayer.scenario.RandomScenario;
import fr.hardel.overstress.fakeplayer.scenario.SpawnerScenario;
import fr.hardel.overstress.fakeplayer.scenario.WanderScenario;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;

public final class BotScenarios {
    public static final ResourceKey<Registry<BotScenario>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Overstress.MOD_ID, "bot_scenario"));
    public static final Registry<BotScenario> REGISTRY = FabricRegistryBuilder.create(KEY).buildAndRegister();
    public static final BotScenario IDLE = new IdleScenario();

    private BotScenarios() {
    }

    public static void bootstrap() {
        register("idle", IDLE);
        register("wander", new WanderScenario());
        register("fly", new FlyScenario());
        register("mine", new MineScenario());
        register("fight", new FightScenario());
        register("spawner", new SpawnerScenario());
        register("dimensions", new DimensionsScenario());
        register("random", new RandomScenario());
    }

    public static BotScenario random(RandomSource random) {
        return REGISTRY.getRandom(random).orElseThrow(() -> new IllegalStateException("No bot scenario is registered")).value();
    }

    private static void register(String path, BotScenario scenario) {
        Registry.register(REGISTRY, Identifier.fromNamespaceAndPath(Overstress.MOD_ID, path), scenario);
    }
}
