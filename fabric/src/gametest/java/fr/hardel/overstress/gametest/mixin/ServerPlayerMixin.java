package fr.hardel.overstress.gametest.mixin;

import fr.hardel.overstress.gametest.ConnectionProbe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "doTick", at = @At("HEAD"))
    private void overstressTest$countDoTick(CallbackInfo callbackInfo) {
        ConnectionProbe.of((ServerPlayer) (Object) this).overstressTest$doTick();
    }

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void overstressTest$countTeleport(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> callbackInfo) {
        ConnectionProbe.of((ServerPlayer) (Object) this).overstressTest$act();
    }
}
