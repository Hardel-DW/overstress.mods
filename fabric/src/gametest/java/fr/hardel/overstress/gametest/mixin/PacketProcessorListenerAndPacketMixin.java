package fr.hardel.overstress.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.hardel.overstress.gametest.ConnectionProbe;
import net.minecraft.network.PacketListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public abstract class PacketProcessorListenerAndPacketMixin {
    @Shadow
    @Final
    private PacketListener listener;

    @WrapMethod(method = "handle")
    private void overstressTest$handleThePacket(Operation<Void> original) {
        if (!(listener instanceof ConnectionProbe probe)) {
            original.call();
            return;
        }

        probe.overstressTest$handling(true);
        original.call();
        probe.overstressTest$handling(false);
    }
}
