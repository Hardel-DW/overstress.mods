package fr.hardel.overstress.gametest.mixin;

import fr.hardel.overstress.gametest.ConnectionProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"))
    private void overstressTest$countDestroy(BlockPos pos, boolean drop, Entity breaker, int updateLimit, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (breaker instanceof ServerPlayer player) {
            ConnectionProbe.of(player).overstressTest$act();
        }
    }
}
