package fr.hardel.overstress;

import fr.hardel.overstress.fakeplayer.FakePlayerCommand;
import fr.hardel.overstress.simulation.SimulationCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

public final class OverstressCommand {
    private OverstressCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> dispatcher.register(Commands.literal(Overstress.MOD_ID)
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(FakePlayerCommand.node())
            .then(SimulationCommand.node())));
    }
}
