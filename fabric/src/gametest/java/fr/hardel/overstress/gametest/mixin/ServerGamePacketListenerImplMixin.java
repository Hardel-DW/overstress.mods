package fr.hardel.overstress.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.hardel.overstress.gametest.TickProbe;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements TickProbe {
    @Unique
    private boolean overstressTest$inTickPlayer;
    @Unique
    private int overstressTest$tickPlayers;
    @Unique
    private int overstressTest$strays;

    @WrapMethod(method = "tickPlayer")
    private boolean overstressTest$countTickPlayer(Operation<Boolean> original) {
        overstressTest$inTickPlayer = true;
        boolean kicked = original.call();
        overstressTest$inTickPlayer = false;
        overstressTest$tickPlayers++;
        return kicked;
    }

    @Override
    public void overstressTest$doTick() {
        if (!overstressTest$inTickPlayer) {
            overstressTest$strays++;
        }
    }

    @Override
    public int overstressTest$tickPlayers() {
        return overstressTest$tickPlayers;
    }

    @Override
    public int overstressTest$strays() {
        return overstressTest$strays;
    }
}
