package fr.hardel.overstress.gametest;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotScenarios;
import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class BotClientTest {
    private static final int JOIN_TICKS = 2400;
    private static final int CONNECTION_TICKS = 40;
    private static final long SERVER_TICK_NANOS = TimeUnit.MILLISECONDS.toNanos(50);
    private static final Component TEST_OVER = Component.literal("test over");

    @GameTest(maxTicks = JOIN_TICKS)
    public void aBotIsTickedOnlyThroughItsConnection(GameTestHelper helper) {
        String name = join(helper, "Tick_Probe", BotScenarios.IDLE);
        helper.succeedWhen(() -> {
            ServerPlayer bot = joined(helper, name);
            TickProbe probe = (TickProbe) bot.connection;
            check(helper, probe.overstressTest$strays() == 0, "the bot ran %d player ticks outside its connection".formatted(probe.overstressTest$strays()));
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

    private static String join(GameTestHelper helper, String name, BotScenario scenario) {
        helper.onEachTick(() -> LockSupport.parkNanos(SERVER_TICK_NANOS));
        FakePlayerManager.spawn(helper.getLevel().getServer(), name, Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO)), scenario, 1);
        return name;
    }

    private static ServerPlayer joined(GameTestHelper helper, String name) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer bot = server.getPlayerList().getPlayerByName(name);
        check(helper, bot != null, "%s has not joined yet".formatted(name));
        return bot;
    }

    private static void check(GameTestHelper helper, boolean holds, String failure) {
        if (!holds) {
            throw helper.assertionException(Component.literal(failure));
        }
    }
}
