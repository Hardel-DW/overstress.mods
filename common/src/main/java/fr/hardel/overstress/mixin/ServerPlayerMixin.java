package fr.hardel.overstress.mixin;

import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A bot runs with its own entity tick, so it inherits whatever thread owns it and makes no assumption
 * about how the server splits its work.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void overstress$tickBot(CallbackInfo callbackInfo) {
        FakePlayerManager.tickBot((ServerPlayer) (Object) this);
    }
}
