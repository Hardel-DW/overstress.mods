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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Load bots: real ServerPlayers through the real login placement, ticked by the level that owns them. */
public final class FakePlayerManager {
    private static final Map<UUID, BotState> bots = new ConcurrentHashMap<>();
    private static final AtomicInteger nextBotId = new AtomicInteger(1);
    private static final RandomSource random = RandomSource.create();

    private FakePlayerManager() {
    }

    /** Each bot draws its own position within {@code spread} of {@code center}, so a fleet lands scattered. */
    public static int spawn(MinecraftServer server, Vec3 center, int count, int spread, BotScenario forced) {
        ServerLevel overworld = server.overworld();
        for (int index = 0; index < count; index++) {
            String name = "Bot_" + nextBotId.getAndIncrement();
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("OverstressBot:" + name).getBytes(StandardCharsets.UTF_8)), name);
            ServerPlayer bot = new ServerPlayer(server, overworld, profile, ClientInformation.createDefault());
            BotScenario scenario = forced != null ? forced : BotScenarios.random(random);
            double x = center.x() + random.nextInt(spread * 2 + 1) - spread + 0.5;
            double z = center.z() + random.nextInt(spread * 2 + 1) - spread + 0.5;
            bots.put(profile.id(), new BotState(scenario, x, z));
            server.getPlayerList().placeNewPlayer(new FakeConnection(), bot, new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false));
            Overstress.LOGGER.info("Spawned {} at [{}, {}] running {}", name, (int) x, (int) z, BotScenarios.REGISTRY.getKey(scenario));
        }

        return count;
    }

    public static int clear(MinecraftServer server) {
        int removed = 0;
        for (UUID id : bots.keySet()) {
            ServerPlayer bot = server.getPlayerList().getPlayer(id);
            if (bot != null) {
                server.getPlayerList().remove(bot);
                removed++;
            }
        }
        bots.clear();

        return removed;
    }

    public static int count() {
        return bots.size();
    }

    /** False when {@code player} is a real player rather than one of ours. */
    public static boolean setScenario(ServerPlayer player, BotScenario scenario) {
        BotState state = bots.get(player.getUUID());
        if (state == null) {
            return false;
        }

        state.scenario = scenario;

        return true;
    }

    /** Gives {@code scenario} to {@code percent} of the fleet and idles the rest; shuffled, so the count is exact. */
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

    /** Snapshot of who runs what, keyed by uuid so the caller resolves its own names. */
    public static Map<UUID, BotScenario> roster() {
        Map<UUID, BotScenario> snapshot = new LinkedHashMap<>();
        bots.forEach((id, state) -> snapshot.put(id, state.scenario));

        return snapshot;
    }

    /** Runs on the thread ticking {@code level}, at the head of its tick. */
    public static void tickLevel(ServerLevel level) {
        for (Map.Entry<UUID, BotState> entry : bots.entrySet()) {
            ServerPlayer bot = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (bot == null || bot.level() != level) {
                continue;
            }

            BotState state = entry.getValue();
            if (!state.initialized) {
                initialize(bot, state);
                continue;
            }

            state.scenario.tick(bot, state, random);
        }
    }

    /**
     * Puts the bot on its drawn column once its own ticket has generated it, never by a blocking load:
     * five hundred bots would mean five hundred blocking generations, and an absent chunk reports the
     * bottom of the world as its height.
     */
    private static void initialize(ServerPlayer bot, BotState state) {
        ServerLevel level = bot.level();
        int blockX = (int) Math.floor(state.spawnX);
        int blockZ = (int) Math.floor(state.spawnZ);
        if (level.getChunkSource().getChunkNow(blockX >> 4, blockZ >> 4) == null) {
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
        state.heading = random.nextDouble() * Math.PI * 2;
        state.initialized = true;
        Overstress.LOGGER.info("{} placed at [{}, {}, {}]", bot.getName().getString(), (int) state.spawnX, (int) floor, (int) state.spawnZ);
    }
}
