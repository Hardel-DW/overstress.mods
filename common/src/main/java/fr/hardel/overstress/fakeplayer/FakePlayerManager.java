package fr.hardel.overstress.fakeplayer;

import com.mojang.authlib.GameProfile;
import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.client.BotClient;
import fr.hardel.overstress.fakeplayer.client.BotClients;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakePlayerManager {
    private static final int COMMAND_CLUSTER_RADIUS = 32;
    private static final Map<UUID, BotClient> bots = new ConcurrentHashMap<>();
    private static final Deque<UUID> arrivals = new ConcurrentLinkedDeque<>();
    private static final List<ArrivalWave> waves = new CopyOnWriteArrayList<>();
    private static final AtomicInteger nextBotId = new AtomicInteger(1);
    private static final RandomSource random = RandomSource.create();
    private static volatile @Nullable BotTransport chosenTransport;
    private static volatile @Nullable BotClients clients;

    private FakePlayerManager() {
    }

    public static void open(MinecraftServer server) {
        clients = new BotClients(server, FakePlayerManager::forget);
    }

    public static void close() {
        BotClients closing = clients;
        clients = null;
        chosenTransport = null;
        waves.clear();
        arrivals.clear();
        bots.clear();
        if (closing != null) {
            closing.close();
        }
    }

    public static BotTransport transport(MinecraftServer server) {
        BotTransport chosen = chosenTransport;
        if (chosen != null) {
            return chosen;
        }

        return server.getPort() >= 0 ? BotTransport.NETWORK : BotTransport.DIRECT;
    }

    public static void transport(BotTransport chosen) {
        chosenTransport = chosen;
    }

    /** A wave lands around the center; with an interval it lands one bot per interval, ticked by the server. */
    public static void spawn(MinecraftServer server, Vec3 center, int count, int spread, int clusterPercent, @Nullable BotScenario forced, int intervalTicks) {
        List<Vec3> placed = bots.values().stream().map(bot -> new Vec3(bot.state().spawnX, 0, bot.state().spawnZ)).toList();
        ClusterSpread cluster = new ClusterSpread(random, clusterPercent, COMMAND_CLUSTER_RADIUS, placed);
        ArrivalWave wave = new ArrivalWave(center, count, spread, cluster, forced, intervalTicks, random, server.getTickCount());
        if (!wave.spawnDue(server, server.getTickCount())) {
            waves.add(wave);
        }
    }

    public static void tick(MinecraftServer server) {
        waves.removeIf(wave -> wave.spawnDue(server, server.getTickCount()));
    }

    public static void assist(ServerPlayer player) {
        BotClient bot = bots.get(player.getUUID());
        if (bot != null) {
            bot.state().scenario().assist(player, bot.state());
        }
    }

    static String nextName() {
        return "Bot_%d".formatted(nextBotId.getAndIncrement());
    }

    /** The bot is a headless client: it joins through the server's login, as if it had logged out at its target, on the ground when that chunk is loaded. */
    public static void spawn(MinecraftServer server, String name, Vec3 position, BotScenario scenario, long seed) {
        GameProfile profile = UUIDUtil.createOfflineProfile(name);
        BotState state = new BotState(scenario, position.x(), position.z(), seed);
        BotTransport transport = transport(server);
        bots.put(profile.id(), clients.join(profile, state, arrival(server.overworld(), position), transport));
        arrivals.addLast(profile.id());
        Overstress.LOGGER.info("{} joins at [{}, {}] running {} over the {} link", name, (int) position.x(), (int) position.z(), BotScenarios.REGISTRY.getKey(scenario),
            transport.getSerializedName());
    }

    private static Vec3 arrival(ServerLevel level, Vec3 position) {
        int x = Mth.floor(position.x());
        int z = Mth.floor(position.z());
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return new Vec3(position.x(), level.getMaxY(), position.z());
        }

        return new Vec3(position.x(), chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1, position.z());
    }

    /** Removes {@code count} bots in the given order; clearing everything drops the pending arrivals too. */
    public static int clear(MinecraftServer server, int count, ClearOrder order) {
        if (count == Integer.MAX_VALUE) {
            waves.clear();
        }

        int removed = 0;
        UUID id;
        while (removed < count && (id = take(order)) != null) {
            if (remove(id)) {
                removed++;
            }
        }

        return removed;
    }

    /** Removes every bot within {@code radius} blocks of the center, the population of one area. */
    public static int clearWithin(MinecraftServer server, Vec3 center, int radius) {
        int removed = 0;
        for (UUID id : new ArrayList<>(arrivals)) {
            ServerPlayer bot = server.getPlayerList().getPlayer(id);
            if (bot != null && bot.position().multiply(1, 0, 1).distanceTo(center.multiply(1, 0, 1)) <= radius) {
                arrivals.remove(id);
                if (remove(id)) {
                    removed++;
                }
            }
        }

        return removed;
    }

    private static @Nullable UUID take(ClearOrder order) {
        return switch (order) {
            case LAST -> arrivals.pollLast();
            case FIRST -> arrivals.pollFirst();
            case RANDOM -> {
                List<UUID> ids = new ArrayList<>(arrivals);
                if (ids.isEmpty()) {
                    yield null;
                }

                UUID picked = ids.get(random.nextInt(ids.size()));
                arrivals.remove(picked);
                yield picked;
            }
        };
    }

    /** The bot leaves as a real client does: it closes its connection, and the server notices it. */
    private static boolean remove(UUID id) {
        BotClient bot = bots.remove(id);
        if (bot == null) {
            return false;
        }

        bot.leave();
        return true;
    }

    /** A bot that left may share its name, so its id, with the bot that replaced it. */
    private static void forget(BotClient bot) {
        if (bots.remove(bot.profile().id(), bot)) {
            arrivals.remove(bot.profile().id());
        }
    }

    public static int count() {
        return bots.size();
    }

    public static boolean setScenario(ServerPlayer player, BotScenario scenario) {
        BotClient bot = bots.get(player.getUUID());
        if (bot == null) {
            return false;
        }

        bot.state().scenario = scenario;
        return true;
    }

    public static int assign(int percent, BotScenario scenario) {
        List<BotClient> shuffled = new ArrayList<>(bots.values());
        for (int index = shuffled.size() - 1; index > 0; index--) {
            Collections.swap(shuffled, index, random.nextInt(index + 1));
        }

        int selected = Math.round(shuffled.size() * percent / 100.0f);
        for (int index = 0; index < shuffled.size(); index++) {
            shuffled.get(index).state().scenario = index < selected ? scenario : BotScenarios.IDLE;
        }

        return selected;
    }

    public static Map<UUID, BotScenario> roster() {
        Map<UUID, BotScenario> snapshot = new LinkedHashMap<>();
        bots.forEach((id, bot) -> snapshot.put(id, bot.state().scenario()));
        return snapshot;
    }

    /** Null for a real player. */
    public static @Nullable ClientChunks clientChunks(ServerPlayer player) {
        BotClient bot = bots.get(player.getUUID());
        return bot == null ? null : bot.chunks();
    }

    public static boolean isBot(SocketAddress address) {
        BotClients current = clients;
        return current != null && current.isBot(address);
    }

    public static void attach(Connection connection) {
        BotClients current = clients;
        if (current != null) {
            current.attach(connection);
        }
    }
}
