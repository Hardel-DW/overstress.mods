package fr.hardel.overstress.gametest.mixin;

import fr.hardel.overstress.gametest.ConnectionProbe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "respawn", at = @At("HEAD"))
    private void overstressTest$countRespawn(ServerPlayer player, boolean keepAllPlayerData, Entity.RemovalReason reason,
        CallbackInfoReturnable<ServerPlayer> callbackInfo) {
        ConnectionProbe.of(player).overstressTest$act();
    }
}
