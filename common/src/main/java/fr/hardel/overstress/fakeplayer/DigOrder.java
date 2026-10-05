package fr.hardel.overstress.fakeplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What the server, which sees the blocks, asks of a bot that only sees the surface: break this block, or walk to this foothold. */
public record DigOrder(@Nullable BlockPos block, @Nullable BlockState state, Vec3 foot) {
}
