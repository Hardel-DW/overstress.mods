package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

public final class MineScenario implements BotScenario {
    private static final double SPEED = 0.15;
    private static final double REACH = Attributes.BLOCK_INTERACTION_RANGE.value().getDefaultValue();
    private static final int DETOUR_TICKS = 20;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        if (state.cooldown > 0) {
            state.cooldown--;
        }

        double heading = state.cooldown > 0 ? state.detour : state.heading;
        BlockPos target = beside(pilot, heading, 1);
        target = target != null ? target : beside(pilot, heading, -1);
        if (target != null) {
            BlockState block = pilot.terrain().block(target);
            pilot.hands().select(pilot.inventory().best(item -> item.getDestroySpeed(block)));
            pilot.look(Vec3.atCenterOf(target));
            pilot.hands().mine(target, Direction.UP);
            return;
        }

        if (pilot.stuck()) {
            state.detour = heading + (random.nextBoolean() ? Math.PI / 2 : -Math.PI / 2);
            state.cooldown = DETOUR_TICKS;
        }

        pilot.walk(state.cooldown > 0 ? state.detour : state.heading, SPEED);
    }

    private static @Nullable BlockPos beside(BotPilot pilot, double heading, int side) {
        int x = Mth.floor(pilot.x() - Math.sin(heading) * side);
        int z = Mth.floor(pilot.z() + Math.cos(heading) * side);
        OptionalInt height = pilot.terrain().height(x, z);
        if (height.isEmpty()) {
            return null;
        }

        BlockPos top = new BlockPos(x, height.getAsInt() - 1, z);
        BlockState block = pilot.terrain().block(top);
        boolean minable = block != null && block.getFluidState().isEmpty() && block.getBlock().defaultDestroyTime() >= 0;
        boolean notBelowPath = top.getY() >= Mth.floor(pilot.position().y) - 1;
        boolean reachable = new AABB(top).distanceToSqr(pilot.eye()) <= REACH * REACH;
        return minable && notBelowPath && reachable ? top : null;
    }
}
