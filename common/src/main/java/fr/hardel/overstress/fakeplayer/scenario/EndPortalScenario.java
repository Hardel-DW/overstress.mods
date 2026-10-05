package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class EndPortalScenario implements BotScenario {
    private static final int AWAY_TICKS = 100;
    private static final double SPEED = 0.2;

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
        BlockPos exit = state.ready ? EndAssist.exitPortal(level) : null;
        if (exit != null) {
            state.ready = false;
            EndAssist.enter(player, exit);
        }
    }

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        if (state.dimension != pilot.status().dimension()) {
            boolean arrived = state.dimension != null;
            state.dimension = pilot.status().dimension();
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
