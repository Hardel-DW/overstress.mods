package fr.hardel.overstress.fakeplayer;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.scenario.ChurnScenario;
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
    public static final BotScenario WANDER = new WanderScenario();
    public static final BotScenario FLY = new FlyScenario();
    public static final BotScenario MINE = new MineScenario();
    public static final BotScenario FIGHT = new FightScenario();
    public static final BotScenario SPAWNER = new SpawnerScenario();
    public static final BotScenario DIMENSIONS = new DimensionsScenario();
    public static final BotScenario RANDOM = new RandomScenario();
    public static final BotScenario CHURN = new ChurnScenario();

    private BotScenarios() {
    }

    public static void bootstrap() {
        register("idle", IDLE);
        register("wander", WANDER);
        register("fly", FLY);
        register("mine", MINE);
        register("fight", FIGHT);
        register("spawner", SPAWNER);
        register("dimensions", DIMENSIONS);
        register("random", RANDOM);
        register("churn", CHURN);
    }

    public static BotScenario random(RandomSource random) {
        return REGISTRY.getRandom(random).orElseThrow(() -> new IllegalStateException("No bot scenario is registered")).value();
    }

    private static void register(String path, BotScenario scenario) {
        Registry.register(REGISTRY, Identifier.fromNamespaceAndPath(Overstress.MOD_ID, path), scenario);
    }
}
