package fr.hardel.overstress.gametest.mixin;

import fr.hardel.overstress.gametest.ConnectionProbe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void overstressTest$countAttack(Entity target, CallbackInfo callbackInfo) {
        if ((Object) this instanceof ServerPlayer player) {
            ConnectionProbe.of(player).overstressTest$act();
        }
    }
}
