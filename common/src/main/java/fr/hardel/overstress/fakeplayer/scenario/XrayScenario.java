package fr.hardel.overstress.fakeplayer.scenario;

import fr.hardel.overstress.fakeplayer.BotScenario;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.DigOrder;
import fr.hardel.overstress.fakeplayer.client.BotPilot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** A cheater to test a detection against: the server tells it where the nearest diamond ore hides, and it tunnels straight to it. */
public final class XrayScenario implements BotScenario {
    private static final TagKey<Block> DIAMOND_ORES = BlockItemTags.DIAMOND_ORES.block();
    private static final double SPEED = 0.25;
    private static final int STALL_TICKS = 100;
    private static final double ARRIVED = 1.0E-4;
    private static final int SIGHT_CHUNKS = 4;
    private static final double REACH = 2.3;
    private static final int PATIENCE_STEPS = 600;
    private static final int BLIND_STEPS = 16;
    private static final int EFFICIENCY = 10;
    private static final int PICKAXE_SLOT = 0;

    /** The bot obeys the order: it breaks the block with real packets, or hovers to the cell the server chose. */
    @Override
    public void steer(BotPilot pilot, BotState state, RandomSource random) {
        DigOrder order = state.order;
        if (order == null) {
            return;
        }

        if (order != state.seen) {
            state.seen = order;
            state.stall = 0;
        }

        BlockPos target = order.block();
        BlockState block = order.state();
        if (target != null && block != null && order != state.done) {
            pilot.hands().select(PICKAXE_SLOT);
            pilot.look(Vec3.atCenterOf(target));
            pilot.hoverTo(pilot.position(), 0);
            if (pilot.hands().mine(target, Direction.UP, block)) {
                state.done = order;
            }

            return;
        }

        if (++state.stall > STALL_TICKS) {
            state.order = null;
            return;
        }

        pilot.hoverTo(order.foot(), SPEED);
    }

    /** A new order only when the last one is over: its block is gone, or the bot is in its cell. */
    @Override
    public void assist(ServerPlayer player, BotState state) {
        ServerLevel level = player.level();
        if (!player.getAbilities().mayfly) {
            equip(player);
        }

        DigOrder order = state.order;
        BlockPos target = order == null ? null : order.block();
        boolean busy = order != null && (target != null ? level.getBlockState(target) == order.state() : player.position().distanceToSqr(order.foot()) > ARRIVED);
        if (!busy) {
            state.order = plan(level, player, state);
        }
    }

    /** It flies, so no hole, no cave and no liquid stops it, and its pickaxe breaks stone at once. */
    private static void equip(ServerPlayer player) {
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        ItemStack pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
        pickaxe.enchant(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), EFFICIENCY);
        pickaxe.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        player.getInventory().setItem(PICKAXE_SLOT, pickaxe);
    }

    /**
     * Within reach the bot breaks the ore. Under the sky it is put on the ore, in a pocket cleared for it. Underground it moves one cell closer,
     * along the axis that has the most left to cover, and breaks what fills that cell. Without any ore around it tunnels ahead and looks again later.
     */
    private static DigOrder plan(ServerLevel level, ServerPlayer player, BotState state) {
        BlockPos feet = player.blockPosition();
        BlockPos lode = lode(level, feet, state);
        if (lode == null) {
            Direction ahead = Direction.getApproximateNearest(Math.cos(state.heading), 0, Math.sin(state.heading));
            DigOrder tunnel = advance(level, player, feet, ahead);
            state.heading += tunnel == null ? Math.PI / 2 : 0;
            return tunnel == null ? stay(player) : tunnel;
        }

        if (lode.distToCenterSqr(player.getEyePosition()) <= REACH * REACH) {
            return new DigOrder(lode, level.getBlockState(lode), player.position());
        }

        if (level.canSeeSky(feet)) {
            Stream.of(lode.above(), lode.above(2)).forEach(cell -> level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState()));
            player.teleportTo(lode.getX() + 0.5, lode.getY() + 1, lode.getZ() + 0.5);
            return stay(player);
        }

        DigOrder closer = ++state.chase > PATIENCE_STEPS ? null : toward(feet, lode).map(step -> advance(level, player, feet, step)).filter(order -> order != null).findFirst().orElse(null);
        if (closer == null) {
            state.refused.add(lode.asLong());
            state.lode = null;
            return stay(player);
        }

        return closer;
    }

    /** The ore the bot was after while it still stands, else the nearest one in the chunks around, at most once every few steps. */
    private static @Nullable BlockPos lode(ServerLevel level, BlockPos feet, BotState state) {
        if (state.lode != null && level.getBlockState(state.lode).is(DIAMOND_ORES)) {
            return state.lode;
        }

        if (state.lode == null && state.trip-- > 0) {
            return null;
        }

        state.lode = nearest(level, feet, state);
        state.chase = 0;
        state.trip = BLIND_STEPS;
        return state.lode;
    }

    private static @Nullable BlockPos nearest(ServerLevel level, BlockPos feet, BotState state) {
        ChunkPos center = ChunkPos.containing(feet);
        BlockPos nearest = null;
        for (int chunkX = center.x() - SIGHT_CHUNKS; chunkX <= center.x() + SIGHT_CHUNKS; chunkX++) {
            for (int chunkZ = center.z() - SIGHT_CHUNKS; chunkZ <= center.z() + SIGHT_CHUNKS; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk != null) {
                    nearest = nearer(chunk, feet, state, nearest);
                }
            }
        }

        return nearest;
    }

    private static @Nullable BlockPos nearer(LevelChunk chunk, BlockPos feet, BotState state, @Nullable BlockPos nearest) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            if (sections[index].hasOnlyAir() || !sections[index].maybeHas(block -> block.is(DIAMOND_ORES))) {
                continue;
            }

            BlockPos corner = chunk.getPos().getBlockAt(0, SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index)), 0);
            for (BlockPos cell : BlockPos.betweenClosed(corner, corner.offset(SectionPos.SECTION_SIZE - 1, SectionPos.SECTION_SIZE - 1, SectionPos.SECTION_SIZE - 1))) {
                boolean wanted = sections[index].getBlockState(cell.getX() & 15, cell.getY() & 15, cell.getZ() & 15).is(DIAMOND_ORES) && !state.refused.contains(cell.asLong());
                if (wanted && (nearest == null || cell.distSqr(feet) < nearest.distSqr(feet))) {
                    nearest = cell.immutable();
                }
            }
        }

        return nearest;
    }

    /** The directions that bring the bot closer, the longest way first. An ore at head height needs no climb. */
    private static Stream<Direction> toward(BlockPos feet, BlockPos lode) {
        BlockPos gap = lode.subtract(feet);
        int climb = gap.getY() == 1 ? 0 : gap.getY();
        return Stream.of(Direction.Axis.values()).filter(axis -> (axis == Direction.Axis.Y ? climb : gap.get(axis)) != 0)
            .sorted(Comparator.comparingInt((Direction.Axis axis) -> Math.abs(gap.get(axis))).reversed())
            .map(axis -> Direction.fromAxisAndDirection(axis, gap.get(axis) > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE));
    }

    /** Null when a block that cannot be broken fills the way. A liquid has no collision: the bot never digs it and passes through. */
    private static @Nullable DigOrder advance(ServerLevel level, ServerPlayer player, BlockPos feet, Direction step) {
        BlockPos next = feet.relative(step);
        for (BlockPos cell : List.of(feet.above(), feet, next.above(), next)) {
            BlockState block = level.getBlockState(cell);
            if (!block.getCollisionShape(level, cell).isEmpty()) {
                return block.getDestroySpeed(level, cell) < 0 ? null : new DigOrder(cell, block, player.position());
            }
        }

        return new DigOrder(null, null, Vec3.atBottomCenterOf(next));
    }

    private static DigOrder stay(ServerPlayer player) {
        return new DigOrder(null, null, player.position());
    }
}
