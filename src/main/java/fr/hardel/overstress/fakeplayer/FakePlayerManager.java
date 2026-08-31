package fr.hardel.overstress.fakeplayer;

import com.mojang.authlib.GameProfile;
import fr.hardel.overstress.Overstress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakePlayerManager {
    private static final int COMMAND_CLUSTER_RADIUS = 32;
    private static final Map<UUID, BotState> bots = new ConcurrentHashMap<>();
    private static final Deque<UUID> arrivals = new ConcurrentLinkedDeque<>();
    private static final AtomicInteger nextBotId = new AtomicInteger(1);
    private static final RandomSource random = RandomSource.create();

    private FakePlayerManager() {
    }

    public static int spawn(MinecraftServer server, Vec3 center, int count, int spread, int clusterPercent, BotScenario forced) {
        ClusterSpread cluster = new ClusterSpread(random, clusterPercent, COMMAND_CLUSTER_RADIUS,
            bots.values().stream().map(state -> new Vec3(state.spawnX, 0, state.spawnZ)).toList());
        for (int index = 0; index < count; index++) {
            double x = center.x() + random.nextInt(spread * 2 + 1) - spread + 0.5;
            double z = center.z() + random.nextInt(spread * 2 + 1) - spread + 0.5;
            spawn(server, "Bot_" + nextBotId.getAndIncrement(), cluster.next(new Vec3(x, 0, z)), forced != null ? forced : BotScenarios.random(random), random.nextLong());
        }

        return count;
    }

    public static void spawn(MinecraftServer server, String name, Vec3 position, BotScenario scenario, long seed) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("OverstressBot:" + name).getBytes(StandardCharsets.UTF_8)), name);
        ClientInformation information = information(server);
        ServerPlayer bot = new ServerPlayer(server, server.overworld(), profile, information);
        bots.put(profile.id(), new BotState(scenario, position.x(), position.z(), seed));
        arrivals.addLast(profile.id());
        server.getPlayerList().placeNewPlayer(new FakeConnection(), bot, new CommonListenerCookie(profile, 0, information, false));
        Overstress.LOGGER.info("Spawned {} at [{}, {}] running {}", name, (int) position.x(), (int) position.z(), BotScenarios.REGISTRY.getKey(scenario));
    }

    /** A bot sees as far as the server allows, like a client with its slider at maximum; the default would ask for 2 chunks. */
    private static ClientInformation information(MinecraftServer server) {
        ClientInformation defaults = ClientInformation.createDefault();
        return new ClientInformation(defaults.language(), server.getPlayerList().getViewDistance(), defaults.chatVisibility(), defaults.chatColors(), defaults.modelCustomisation(),
            defaults.mainHand(), defaults.textFilteringEnabled(), defaults.allowsListing(), defaults.particleStatus());
    }

    /** Removes the {@code count} most recent bots, the wave that just arrived while the older ones stay. */
    public static int clear(MinecraftServer server, int count) {
        int removed = 0;
        UUID id;
        while (removed < count && (id = arrivals.pollLast()) != null) {
            bots.remove(id);
            ServerPlayer bot = server.getPlayerList().getPlayer(id);
            if (bot != null) {
                server.getPlayerList().remove(bot);
                removed++;
            }
        }

        return removed;
    }

    public static int count() {
        return bots.size();
    }

    public static boolean setScenario(ServerPlayer player, BotScenario scenario) {
        BotState state = bots.get(player.getUUID());
        if (state == null) {
            return false;
        }

        state.scenario = scenario;

        return true;
    }

    public static int assign(int percent, BotScenario scenario) {
        List<UUID> ids = new ArrayList<>(bots.keySet());
        for (int index = ids.size() - 1; index > 0; index--) {
            Collections.swap(ids, index, random.nextInt(index + 1));
        }

        int selected = Math.round(ids.size() * percent / 100.0f);
        for (int index = 0; index < ids.size(); index++) {
            BotState state = bots.get(ids.get(index));
            if (state != null) {
                state.scenario = index < selected ? scenario : BotScenarios.IDLE;
            }
        }

        return selected;
    }

    public static Map<UUID, BotScenario> roster() {
        Map<UUID, BotScenario> snapshot = new LinkedHashMap<>();
        bots.forEach((id, state) -> snapshot.put(id, state.scenario));

        return snapshot;
    }

    public static void tickBot(ServerPlayer bot) {
        BotState state = bots.get(bot.getUUID());
        if (state == null) {
            return;
        }

        if (!state.initialized) {
            initialize(bot, state);
            return;
        }

        Vec3 held = bot.position();
        bot.doTick();
        bot.absSnapTo(held.x, held.y, held.z, bot.getYRot(), bot.getXRot());
        state.scenario.tick(bot, state, state.random);
    }

    private static void initialize(ServerPlayer bot, BotState state) {
        ServerLevel level = bot.level();
        int blockX = (int) Math.floor(state.spawnX);
        int blockZ = (int) Math.floor(state.spawnZ);
        if (!level.hasChunk(blockX >> 4, blockZ >> 4)) {
            BotMovement.place(bot, state.spawnX, bot.getY(), state.spawnZ, 0);
            return;
        }

        double floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
        BotMovement.place(bot, state.spawnX, floor, state.spawnZ, 0);
        bot.setInvulnerable(true);
        AttributeInstance stepHeight = bot.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            stepHeight.setBaseValue(20);
        }
        state.heading = state.random.nextDouble() * Math.PI * 2;
        state.initialized = true;
        Overstress.LOGGER.info("{} placed at [{}, {}, {}]", bot.getName().getString(), (int) state.spawnX, (int) floor, (int) state.spawnZ);
    }
}
