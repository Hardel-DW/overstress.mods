package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EndGatewayScenario implements BotScenario {
    private static final int AWAY_TICKS = 100;
    private static final double SPEED = 0.2;
    private static final double HOP = 64;
    private static final double MAIN_ISLAND = 200;

    @Override
    public void prepare(ServerLevel level, BotState state) {
        EndAssist.buildPortal(level, state);
    }

    @Override
    public void assist(ServerPlayer player, BotState state) {
        ServerLevel level = player.level();
        if (level.dimension() != Level.END) {
            EndAssist.bringHome(player, state);
            return;
        }

        EndAssist.killDragons(level);
        Vec3 arrival = state.portal;
        if (!state.ready || arrival == null) {
            return;
        }

        boolean onMainIsland = arrival.horizontalDistanceSqr() < MAIN_ISLAND * MAIN_ISLAND;
        BlockPos gateway = onMainIsland ? EndAssist.ringGateway(level, player.position()) : EndAssist.gatewayNear(level, arrival);
        if (gateway != null) {
            state.ready = false;
            EndAssist.enter(player, gateway);
        }
    }

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        Vec3 now = pilot.position();
        boolean hopped = state.last != null && now.distanceToSqr(state.last) > HOP * HOP;
        state.last = now;
        if (state.dimension != pilot.status().dimension() || hopped) {
            boolean arrived = state.dimension != null;
            state.dimension = pilot.status().dimension();
            state.portal = now;
            state.cooldown = arrived ? AWAY_TICKS : 0;
            state.ready = false;
        }

        if (state.cooldown > 0) {
            state.cooldown--;
            pilot.walk(state.heading, SPEED);
            return;
        }

        if (state.dimension == Level.END) {
            state.ready = true;
            return;
        }

        if (state.home != null) {
            pilot.walkTo(state.home, SPEED);
        }
    }
}
