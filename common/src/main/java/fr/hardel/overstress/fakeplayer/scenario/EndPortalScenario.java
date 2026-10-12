package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

public final class EndPortalScenario implements BotScenario {
    public static final BotScenario EXIT = new EndPortalScenario(EndAssist::exitPortal);
    public static final BotScenario GATEWAYS = new EndPortalScenario(EndAssist::gateway);
    private static final int STAY_TICKS = 200;
    private static final int RETRY_TICKS = 20;
    private final BiFunction<ServerLevel, ServerPlayer, @Nullable BlockPos> wayOut;

    private EndPortalScenario(BiFunction<ServerLevel, ServerPlayer, @Nullable BlockPos> wayOut) {
        this.wayOut = wayOut;
    }

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
    }

    @Override
    public void assist(ServerPlayer player, BotState state) {
        ServerLevel level = player.level();
        if (state.arrives(level.dimension())) {
            state.trip = STAY_TICKS;
            return;
        }

        boolean inTheEnd = level.dimension() == Level.END;
        if (inTheEnd) {
            EndAssist.killDragons(level);
        }

        if (--state.trip > 0) {
            return;
        }

        BlockPos portal = inTheEnd ? wayOut.apply(level, player) : EndAssist.home(level, player, state);
        state.trip = portal == null ? RETRY_TICKS : STAY_TICKS;
        if (portal != null) {
            Portals.enter(player, portal);
        }
    }
}
