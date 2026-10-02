package fr.hardel.overstress.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.hardel.overstress.gametest.ConnectionProbe;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements ConnectionProbe {
    @Unique
    private boolean overstressTest$inTickPlayer;
    @Unique
    private boolean overstressTest$handling;
    @Unique
    private int overstressTest$tickPlayers;
    @Unique
    private int overstressTest$strayTicks;
    @Unique
    private int overstressTest$actions;
    @Unique
    private int overstressTest$strayActions;

    @WrapMethod(method = "tickPlayer")
    private boolean overstressTest$countTickPlayer(Operation<Boolean> original) {
        overstressTest$inTickPlayer = true;
        boolean kicked = original.call();
        overstressTest$inTickPlayer = false;
        overstressTest$tickPlayers++;
        return kicked;
    }

    @WrapMethod(method = "performUnsignedChatCommand")
    private void overstressTest$handleTheCommand(String command, Operation<Void> original) {
        overstressTest$handling(true);
        original.call(command);
        overstressTest$handling(false);
    }

    @Override
    public void overstressTest$doTick() {
        if (!overstressTest$inTickPlayer) {
            overstressTest$strayTicks++;
        }
    }

    @Override
    public void overstressTest$act() {
        if (overstressTest$handling) {
            overstressTest$actions++;
            return;
        }

        overstressTest$strayActions++;
    }

    @Override
    public void overstressTest$handling(boolean handling) {
        overstressTest$handling = handling;
    }

    @Override
    public int overstressTest$tickPlayers() {
        return overstressTest$tickPlayers;
    }

    @Override
    public int overstressTest$strayTicks() {
        return overstressTest$strayTicks;
    }

    @Override
    public int overstressTest$actions() {
        return overstressTest$actions;
    }

    @Override
    public int overstressTest$strayActions() {
        return overstressTest$strayActions;
    }
}
