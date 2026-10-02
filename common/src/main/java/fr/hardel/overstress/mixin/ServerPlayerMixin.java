package fr.hardel.overstress.mixin;

import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server side of a scenario acts in the entity tick of its bot, so it runs on whatever thread owns the player.
 * The bot itself moves and ticks like any player, through its connection.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void overstress$actForTheBot(CallbackInfo callbackInfo) {
        FakePlayerManager.act((ServerPlayer) (Object) this);
    }
}
