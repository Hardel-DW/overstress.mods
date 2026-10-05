package fr.hardel.overstress.mixin;

import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void overstress$assistTheBot(CallbackInfo callbackInfo) {
        FakePlayerManager.assist((ServerPlayer) (Object) this);
    }
}
