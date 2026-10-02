package fr.hardel.overstress.gametest.mixin;

import fr.hardel.overstress.gametest.TickProbe;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "doTick", at = @At("HEAD"))
    private void overstressTest$countDoTick(CallbackInfo callbackInfo) {
        ((TickProbe) ((ServerPlayer) (Object) this).connection).overstressTest$doTick();
    }
}
