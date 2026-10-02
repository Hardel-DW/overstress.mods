package fr.hardel.overstress.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @WrapMethod(method = "getProfilePermissions")
    private LevelBasedPermissionSet overstress$botsWhoseScenarioRunsCommands(NameAndId nameAndId, Operation<LevelBasedPermissionSet> original) {
        return FakePlayerManager.isOperator(nameAndId) ? LevelBasedPermissionSet.GAMEMASTER : original.call(nameAndId);
    }
}
