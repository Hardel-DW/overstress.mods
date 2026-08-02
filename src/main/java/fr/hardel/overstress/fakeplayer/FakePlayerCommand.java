package fr.hardel.overstress.fakeplayer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** The whole user surface of the mod: {@code /fakeplayer spawn|clear|list}, gamemaster level. */
public final class FakePlayerCommand {

    private static final DynamicCommandExceptionType UNKNOWN_SCENARIO = new DynamicCommandExceptionType(id -> Component.literal("Unknown bot scenario '" + id + "'"));
    private static final SuggestionProvider<CommandSourceStack> SCENARIOS = (_, builder) -> SharedSuggestionProvider.suggestResource(BotScenarios.REGISTRY.keySet(), builder);

    private FakePlayerCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> registerTree(dispatcher));
    }

    private static void registerTree(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fakeplayer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("spawn")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 500))
                    .then(Commands.argument("spread", IntegerArgumentType.integer(16, 100_000))
                        .executes(context -> spawn(context.getSource(), IntegerArgumentType.getInteger(context, "count"), IntegerArgumentType.getInteger(context, "spread"), null))
                        .then(Commands.argument("scenario", IdentifierArgument.id()).suggests(SCENARIOS)
                            .executes(context -> spawn(context.getSource(), IntegerArgumentType.getInteger(context, "count"), IntegerArgumentType.getInteger(context, "spread"), IdentifierArgument.getId(context, "scenario")))))))
            .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
            .then(Commands.literal("list").executes(context -> list(context.getSource()))));
    }

    private static int spawn(CommandSourceStack source, int count, int spread, Identifier scenarioId) throws CommandSyntaxException {
        BotScenario scenario = scenarioId == null ? null : resolve(scenarioId);
        int spawned = FakePlayerManager.spawn(source.getServer(), source.getPosition(), count, spread, scenario);
        source.sendSuccess(() -> Component.literal("Spawned " + spawned + " bot" + (spawned == 1 ? "" : "s") + " within " + spread + " blocks" + (scenarioId == null ? ", random scenarios" : ", scenario " + scenarioId)), true);

        return spawned;
    }

    private static BotScenario resolve(Identifier scenarioId) throws CommandSyntaxException {
        BotScenario scenario = BotScenarios.REGISTRY.getValue(scenarioId);
        if (scenario == null) {
            throw UNKNOWN_SCENARIO.create(scenarioId);
        }

        return scenario;
    }

    private static int clear(CommandSourceStack source) {
        int removed = FakePlayerManager.clear(source.getServer());
        source.sendSuccess(() -> Component.literal("Removed " + removed + " bots"), true);

        return removed;
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(FakePlayerManager.count() + " bots active"), false);

        return FakePlayerManager.count();
    }
}
