package fr.hardel.overstress.fakeplayer;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.scenario.ChurnScenario;
import fr.hardel.overstress.fakeplayer.scenario.ElytraScenario;
import fr.hardel.overstress.fakeplayer.scenario.EndPortalScenario;
import fr.hardel.overstress.fakeplayer.scenario.FightScenario;
import fr.hardel.overstress.fakeplayer.scenario.IdleScenario;
import fr.hardel.overstress.fakeplayer.scenario.KillauraScenario;
import fr.hardel.overstress.fakeplayer.scenario.MineScenario;
import fr.hardel.overstress.fakeplayer.scenario.NetherPortalScenario;
import fr.hardel.overstress.fakeplayer.scenario.PvpScenario;
import fr.hardel.overstress.fakeplayer.scenario.RandomScenario;
import fr.hardel.overstress.fakeplayer.scenario.ReachScenario;
import fr.hardel.overstress.fakeplayer.scenario.SpawnerScenario;
import fr.hardel.overstress.fakeplayer.scenario.WanderScenario;
import fr.hardel.overstress.fakeplayer.scenario.XrayScenario;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

public final class BotScenarios {
    public static final ResourceKey<Registry<BotScenario>> KEY = ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Overstress.MOD_ID, "bot_scenario"));
    public static final Registry<BotScenario> REGISTRY = new MappedRegistry<>(KEY, Lifecycle.stable());
    public static final BotScenario IDLE = new IdleScenario();
    public static final BotScenario WANDER = new WanderScenario();
    public static final BotScenario ELYTRA = new ElytraScenario();
    public static final BotScenario MINE = new MineScenario();
    public static final BotScenario FIGHT = new FightScenario();
    public static final BotScenario SPAWNER = new SpawnerScenario();
    public static final BotScenario NETHER = new NetherPortalScenario();
    public static final BotScenario END = EndPortalScenario.EXIT;
    public static final BotScenario GATEWAY = EndPortalScenario.GATEWAYS;
    public static final BotScenario RANDOM = new RandomScenario();
    public static final BotScenario CHURN = new ChurnScenario();
    public static final BotScenario PVP = new PvpScenario();
    public static final BotScenario XRAY = new XrayScenario();
    public static final BotScenario KILLAURA = new KillauraScenario();
    public static final BotScenario REACH = new ReachScenario();
    private static final List<BotScenario> DRAWN = new ArrayList<>();

    private BotScenarios() {
    }

    public static void bootstrap() {
        register("idle", IDLE);
        register("wander", WANDER);
        register("elytra", ELYTRA);
        register("mine", MINE);
        register("fight", FIGHT);
        register("spawner", SPAWNER);
        register("nether", NETHER);
        register("end", END);
        register("gateway", GATEWAY);
        register("random", RANDOM);
        register("churn", CHURN);
        register("pvp", PVP);
        cheat("xray", XRAY);
        cheat("killaura", KILLAURA);
        cheat("reach", REACH);
    }

    public static BotScenario random(RandomSource random) {
        return Util.getRandom(DRAWN, random);
    }

    private static void register(String path, BotScenario scenario) {
        cheat(path, scenario);
        DRAWN.add(scenario);
    }

    /** A cheater is there to test a detection: it only plays when asked by name, never in a random draw. */
    private static void cheat(String path, BotScenario scenario) {
        Registry.register(REGISTRY, Identifier.fromNamespaceAndPath(Overstress.MOD_ID, path), scenario);
    }
}
