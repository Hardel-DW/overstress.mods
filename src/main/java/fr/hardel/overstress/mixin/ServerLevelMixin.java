package fr.hardel.overstress.mixin;

import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * The only hook: bots tick with their level, on whatever thread runs that level tick. This targets
 * the level tick itself rather than a Fabric tick event on purpose. On a regionised server the bots
 * must load the region that owns them, and mod-facing tick events are exactly what such a server is
 * free to reroute onto a global thread.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void overstress$tickBots(BooleanSupplier haveTime, CallbackInfo callbackInfo) {
        FakePlayerManager.tickLevel((ServerLevel) (Object) this);
    }
}
