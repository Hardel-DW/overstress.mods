package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

public final class MineScenario implements BotScenario {
    private static final int TURN_TICKS = 8;
    private static final double SPEED = 0.15;
    private static final double REACH = Attributes.BLOCK_INTERACTION_RANGE.value().getDefaultValue();
    private static final int AHEAD = 2;
    private static final int SPREAD = 1;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        BlockPos target = target(pilot, state);
        if (target != null) {
            BlockState block = pilot.terrain().block(target);
            pilot.hands().select(pilot.inventory().best(item -> item.getDestroySpeed(block)));
            pilot.look(Vec3.atCenterOf(target));
            pilot.hands().mine(target, Direction.UP);
            return;
        }

        pilot.walk(state.heading, SPEED);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = TURN_TICKS;
        state.heading += (random.nextDouble() - 0.5) * 0.6;
    }

    private static @Nullable BlockPos target(BotPilot pilot, BotState state) {
        int aheadX = Mth.floor(pilot.x() + Math.cos(state.heading) * AHEAD);
        int aheadZ = Mth.floor(pilot.z() + Math.sin(state.heading) * AHEAD);
        BlockPos nearest = null;
        double reached = REACH * REACH;
        for (int x = aheadX - SPREAD; x <= aheadX + SPREAD; x++) {
            for (int z = aheadZ - SPREAD; z <= aheadZ + SPREAD; z++) {
                BlockPos top = groundTop(pilot, x, z);
                double distance = top == null ? Double.MAX_VALUE : new AABB(top).distanceToSqr(pilot.eye());
                if (distance < reached) {
                    nearest = top;
                    reached = distance;
                }
            }
        }

        return nearest;
    }

    private static @Nullable BlockPos groundTop(BotPilot pilot, int x, int z) {
        BlockState top = pilot.terrain().top(x, z);
        OptionalInt height = pilot.terrain().height(x, z);
        boolean ground = top != null && (top.is(BlockTags.MINEABLE_WITH_SHOVEL) || top.is(BlockTags.MINEABLE_WITH_PICKAXE));
        return ground ? new BlockPos(x, height.getAsInt() - 1, z) : null;
    }
}
