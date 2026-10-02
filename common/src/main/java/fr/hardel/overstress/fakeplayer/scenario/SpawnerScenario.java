package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotStanding;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalInt;

public final class SpawnerScenario implements BotScenario {
    private static final BotStanding CREATIVE = new BotStanding(GameType.CREATIVE, true, false);
    private static final int CLICK_TICKS = 2;
    private static final int FARM_MOBS = 64;
    private static final double NEARBY_WIDTH = 32;
    private static final double NEARBY_HEIGHT = 16;
    private static final double ROUND_RADIUS = 6;
    private static final double ROUND_STEP = 0.3;
    private static final double SPEED = 0.1;
    private static final int THROW_RADIUS = 3;

    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        double angle = Math.atan2(pilot.z() - state.spawnZ, pilot.x() - state.spawnX) + ROUND_STEP;
        pilot.walkTo(new Vec3(state.spawnX + Math.cos(angle) * ROUND_RADIUS, 0, state.spawnZ + Math.sin(angle) * ROUND_RADIUS), SPEED);
        if (--state.cooldown > 0) {
            return;
        }

        state.cooldown = CLICK_TICKS;
        int nearby = pilot.entities().count(AABB.ofSize(pilot.position(), NEARBY_WIDTH, NEARBY_HEIGHT, NEARBY_WIDTH), entity -> entity.type().getCategory() != MobCategory.MISC);
        OptionalInt egg = pilot.inventory().hotbar(item -> item.is(Items.ZOMBIE_SPAWN_EGG));
        int x = Mth.floor(pilot.x()) + random.nextInt(THROW_RADIUS * 2 + 1) - THROW_RADIUS;
        int z = Mth.floor(pilot.z()) + random.nextInt(THROW_RADIUS * 2 + 1) - THROW_RADIUS;
        OptionalInt height = pilot.terrain().height(x, z);
        if (nearby >= FARM_MOBS || egg.isEmpty() || height.isEmpty()) {
            return;
        }

        BlockPos ground = new BlockPos(x, height.getAsInt() - 1, z);
        pilot.hands().select(egg.getAsInt());
        pilot.look(Vec3.atCenterOf(ground).add(0, 0.5, 0));
        pilot.hands().useOn(ground, Direction.UP);
    }

    @Override
    public BotStanding standing() {
        return CREATIVE;
    }
}
