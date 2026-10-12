package fr.hardel.overstress.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @WrapOperation(method = "canPlayerLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canBypassPlayerLimit(Lnet/minecraft/server/players/NameAndId;)Z"))
    private boolean overstress$botsTakeNoSlot(PlayerList playerList, NameAndId nameAndId, Operation<Boolean> original, @Local(argsOnly = true) SocketAddress address) {
        return original.call(playerList, nameAndId) || FakePlayerManager.isBot(address);
    }
}
