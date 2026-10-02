package fr.hardel.overstress.gametest;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotScenarios;
import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public final class BotClientTest {
    private static final int JOIN_TICKS = 2400;
    private static final int CONNECTION_TICKS = 40;
    private static final long SERVER_TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50);
    private static final Component TEST_OVER = Component.literal("test over");
    private static int pacedTick = -1;

    @GameTest(maxTicks = JOIN_TICKS)
    public void aBotIsTickedOnlyThroughItsConnection(GameTestHelper helper) {
        String name = join(helper, "Tick_Probe", BotScenarios.IDLE);
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            ConnectionProbe probe = ConnectionProbe.of(bot);
            check(helper, probe.overstressTest$strayTicks() == 0, "the bot ran %d player ticks outside its connection".formatted(probe.overstressTest$strayTicks()));
            check(helper, probe.overstressTest$tickPlayers() >= CONNECTION_TICKS, "its connection ticked it %d times".formatted(probe.overstressTest$tickPlayers()));
            bot.connection.disconnect(TEST_OVER);
        });
    }

    @GameTest(maxTicks = JOIN_TICKS)
    public void aFlyingBotGlidesThroughItsMovePackets(GameTestHelper helper) {
        String name = join(helper, "Glide_Probe", BotScenarios.FLY);
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            check(helper, bot.isFallFlying(), "the bot is not gliding");
            check(helper, bot.getStats().getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM)) > 0, "the server counted no glided distance");
            bot.connection.disconnect(TEST_OVER);
        });
    }

    @GameTest(maxTicks = JOIN_TICKS)
    public void aMiningBotBreaksBlocksOnlyThroughItsActionPackets(GameTestHelper helper) {
        String name = join(helper, "Mine_Probe", BotScenarios.MINE);
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            checkNoStrayAction(helper, bot);
            check(helper, minedBlocks(bot) > 0, "the server counted no block mined by the bot");
            bot.connection.disconnect(TEST_OVER);
        });
    }

    @GameTest(maxTicks = JOIN_TICKS)
    public void aFightingBotHurtsOnlyThroughItsAttackPackets(GameTestHelper helper) {
        String name = join(helper, "Fight_Probe", BotScenarios.FIGHT);
        BlockPos ground = helper.getLevel().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, helper.absolutePos(new BlockPos(2, 0, 0)));
        Pig pig = helper.spawn(EntityTypes.PIG, helper.relativePos(ground));
        pig.setNoAi(true);
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            checkNoStrayAction(helper, bot);
            check(helper, pig.getHealth() < pig.getMaxHealth(), "the pig took no hit");
            check(helper, ConnectionProbe.of(bot).overstressTest$actions() > 0, "no attack came through the bot's packets");
            bot.connection.disconnect(TEST_OVER);
        });
    }

    @GameTest(maxTicks = JOIN_TICKS)
    public void aSpawningBotSpawnsOnlyWithItsEggs(GameTestHelper helper) {
        String name = join(helper, "Spawner_Probe", BotScenarios.SPAWNER);
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            checkNoStrayAction(helper, bot);
            check(helper, bot.getStats().getValue(Stats.ITEM_USED.get(Items.ZOMBIE_SPAWN_EGG)) > 0, "the bot used no zombie spawn egg");
            bot.connection.disconnect(TEST_OVER);
        });
    }

    @GameTest(maxTicks = JOIN_TICKS)
    public void aTravellingBotChangesDimensionOnlyThroughItsCommands(GameTestHelper helper) {
        String name = join(helper, "Travel_Probe", BotScenarios.DIMENSIONS);
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            checkNoStrayAction(helper, bot);
            check(helper, bot.level().dimension() != Level.OVERWORLD, "the bot is still in the overworld");
            bot.connection.disconnect(TEST_OVER);
        });
    }

    @GameTest(maxTicks = JOIN_TICKS)
    public void aMortalBotDiesAndRespawnsThroughItsClientCommand(GameTestHelper helper) {
        String name = join(helper, "Mortal_Probe", BotScenarios.PVP);
        AtomicReference<ServerPlayer> struck = new AtomicReference<>();
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            check(helper, bot.connection.hasClientLoaded(), "the bot has not loaded its level yet");
            if (struck.get() == null) {
                bot.hurtServer(helper.getLevel(), bot.damageSources().generic(), Float.MAX_VALUE);
                struck.set(bot);
            }

            check(helper, bot != struck.get() && bot.isAlive(), "the bot has not respawned yet");
            checkNoStrayAction(helper, bot);
            check(helper, ConnectionProbe.of(bot).overstressTest$actions() > 0, "the respawn did not come through the bot's packets");
            bot.connection.disconnect(TEST_OVER);
        });
    }

    private static String join(GameTestHelper helper, String name, BotScenario scenario) {
        MinecraftServer server = helper.getLevel().getServer();
        helper.onEachTick(() -> pace(server));
        FakePlayerManager.spawn(server, name, Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO)), scenario, 1);
        return name;
    }

    private static void pace(MinecraftServer server) {
        if (server.getTickCount() != pacedTick) {
            pacedTick = server.getTickCount();
            LockSupport.parkNanos(SERVER_TICK_NANOS);
        }
    }

    private static ServerPlayer joined(GameTestHelper helper, String name) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer bot = server.getPlayerList().getPlayerByName(name);
        check(helper, bot != null, "%s has not joined yet".formatted(name));
        return bot;
    }

    private static void checkNoStrayAction(GameTestHelper helper, ServerPlayer bot) {
        int strays = ConnectionProbe.of(bot).overstressTest$strayActions();
        check(helper, strays == 0, "the server acted %d times for the bot outside its packets".formatted(strays));
    }

    private static int minedBlocks(ServerPlayer bot) {
        return BuiltInRegistries.BLOCK.stream().mapToInt(block -> bot.getStats().getValue(Stats.BLOCK_MINED.get(block))).sum();
    }

    private static void check(GameTestHelper helper, boolean holds, String failure) {
        if (!holds) {
            throw helper.assertionException(Component.literal(failure));
        }
    }
}
