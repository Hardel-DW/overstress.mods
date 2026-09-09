package fr.hardel.overstress;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.hardel.overstress.fakeplayer.FakePlayerCommand;
import fr.hardel.overstress.simulation.SimulationCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class OverstressCommand {
    private OverstressCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal(Overstress.MOD_ID)
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(FakePlayerCommand.node())
            .then(SimulationCommand.node());
    }
}
