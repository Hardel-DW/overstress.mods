package fr.hardel.overstress.simulation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class SimulationCommand {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(id -> Component.literal("Unknown simulation " + id));
    private static final SimpleCommandExceptionType ALREADY_RUNNING = new SimpleCommandExceptionType(Component.literal("A simulation is already running"));
    private static final SimpleCommandExceptionType NOT_RUNNING = new SimpleCommandExceptionType(Component.literal("No simulation is running"));
    private static final SuggestionProvider<CommandSourceStack> SIMULATIONS = (context, builder) -> SharedSuggestionProvider.suggestResource(Simulations.REGISTRY.keySet(), builder);

    private SimulationCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("simulation")
            .then(Commands.literal("start")
                .then(Commands.argument("simulation", IdentifierArgument.id()).suggests(SIMULATIONS)
                    .executes(context -> start(context.getSource(), IdentifierArgument.getId(context, "simulation")))))
            .then(Commands.literal("stop").executes(context -> stop(context.getSource())))
            .then(Commands.literal("status").executes(context -> status(context.getSource())));
    }

    private static int start(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        Simulation simulation = Simulations.REGISTRY.getValue(id);
        if (simulation == null) {
            throw UNKNOWN.create(id);
        }

        if (!SimulationRunner.start(source.getServer(), id, simulation)) {
            throw ALREADY_RUNNING.create();
        }

        source.sendSuccess(() -> Component.literal("Simulation " + id + " started, " + simulation.bots() + " bots for " + simulation.durationTicks() + " ticks"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        if (!SimulationRunner.stop()) {
            throw NOT_RUNNING.create();
        }

        source.sendSuccess(() -> Component.literal("Simulation stopped"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        SimulationRunner runner = SimulationRunner.active();
        if (runner == null) {
            throw NOT_RUNNING.create();
        }

        source.sendSuccess(() -> Component.literal(runner.id() + " running, tick " + runner.elapsedTicks() + " of " + runner.simulation().durationTicks()), false);
        return Command.SINGLE_SUCCESS;
    }
}
