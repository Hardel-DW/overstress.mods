package fr.hardel.overstress.fakeplayer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public final class FakePlayerCommand {

    private static final DynamicCommandExceptionType UNKNOWN_SCENARIO = new DynamicCommandExceptionType(id -> Component.literal("Unknown bot scenario '" + id + "'"));
    private static final DynamicCommandExceptionType NOT_A_BOT = new DynamicCommandExceptionType(name -> Component.literal("'" + name + "' is a real player, not one of our bots"));
    private static final SuggestionProvider<CommandSourceStack> SCENARIOS = (_, builder) -> SharedSuggestionProvider.suggestResource(BotScenarios.REGISTRY.keySet(), builder);

    private FakePlayerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("player")
            .then(Commands.literal("spawn")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 5000))
                    .then(Commands.argument("spread", IntegerArgumentType.integer(16, 100_000))
                        .executes(context -> spawn(context, 0, null, 0))
                        .then(Commands.argument("cluster", IntegerArgumentType.integer(0, 100))
                            .executes(context -> spawn(context, IntegerArgumentType.getInteger(context, "cluster"), null, 0))
                            .then(Commands.argument("scenario", IdentifierArgument.id()).suggests(SCENARIOS)
                                .executes(context -> spawn(context, IntegerArgumentType.getInteger(context, "cluster"), IdentifierArgument.getId(context, "scenario"), 0))
                                .then(Commands.argument("everyTicks", IntegerArgumentType.integer(1))
                                    .executes(context -> spawn(context, IntegerArgumentType.getInteger(context, "cluster"), IdentifierArgument.getId(context, "scenario"), IntegerArgumentType.getInteger(context, "everyTicks")))))))))
            .then(Commands.literal("scenario")
                .then(Commands.literal("set")
                    .then(Commands.argument("bot", EntityArgument.player())
                        .then(Commands.argument("scenario", IdentifierArgument.id()).suggests(SCENARIOS)
                            .executes(context -> setScenario(context.getSource(), EntityArgument.getPlayer(context, "bot"), IdentifierArgument.getId(context, "scenario"))))))
                .then(Commands.literal("random")
                    .then(Commands.argument("percent", IntegerArgumentType.integer(1, 100))
                        .then(Commands.argument("scenario", IdentifierArgument.id()).suggests(SCENARIOS)
                            .executes(context -> assignScenario(context.getSource(), IntegerArgumentType.getInteger(context, "percent"), IdentifierArgument.getId(context, "scenario"))))))
                .then(Commands.literal("list").executes(context -> listScenarios(context.getSource()))))
            .then(Commands.literal("clear").executes(context -> clear(context.getSource(), Integer.MAX_VALUE, ClearOrder.LAST))
                .then(Commands.literal("within")
                    .then(Commands.argument("radius", IntegerArgumentType.integer(1)).executes(context -> clearWithin(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                    .executes(context -> clear(context.getSource(), IntegerArgumentType.getInteger(context, "count"), ClearOrder.LAST))
                    .then(Commands.literal("first").executes(context -> clear(context.getSource(), IntegerArgumentType.getInteger(context, "count"), ClearOrder.FIRST)))
                    .then(Commands.literal("last").executes(context -> clear(context.getSource(), IntegerArgumentType.getInteger(context, "count"), ClearOrder.LAST)))
                    .then(Commands.literal("random").executes(context -> clear(context.getSource(), IntegerArgumentType.getInteger(context, "count"), ClearOrder.RANDOM)))))
            .then(Commands.literal("pos").executes(context -> positions(context.getSource())))
            .then(Commands.literal("list").executes(context -> list(context.getSource())));
    }

    private static int spawn(CommandContext<CommandSourceStack> context, int cluster, Identifier scenarioId, int everyTicks) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int count = IntegerArgumentType.getInteger(context, "count");
        int spread = IntegerArgumentType.getInteger(context, "spread");
        BotScenario scenario = scenarioId == null ? null : resolve(scenarioId);
        FakePlayerManager.spawn(source.getServer(), source.getPosition(), count, spread, cluster, scenario, everyTicks);
        String pace = everyTicks > 0 ? ", one every " + everyTicks + " ticks" : "";
        source.sendSuccess(() -> Component.literal("Spawning " + count + " bots within " + spread + " blocks, " + cluster + "% clustered" + (scenarioId == null ? ", random scenarios" : ", scenario " + scenarioId) + pace), true);

        return count;
    }

    private static int setScenario(CommandSourceStack source, ServerPlayer bot, Identifier scenarioId) throws CommandSyntaxException {
        String name = bot.getName().getString();
        if (!FakePlayerManager.setScenario(bot, resolve(scenarioId))) {
            throw NOT_A_BOT.create(name);
        }

        source.sendSuccess(() -> Component.literal(name + " now runs " + scenarioId), true);
        return 1;
    }

    private static int assignScenario(CommandSourceStack source, int percent, Identifier scenarioId) throws CommandSyntaxException {
        int selected = FakePlayerManager.assign(percent, resolve(scenarioId));
        int idled = FakePlayerManager.count() - selected;
        source.sendSuccess(() -> Component.literal(selected + " bot" + (selected == 1 ? "" : "s") + " now run " + scenarioId + ", the other " + idled + " went idle"), true);
        return selected;
    }

    private static int listScenarios(CommandSourceStack source) {
        Map<UUID, BotScenario> roster = FakePlayerManager.roster();
        Map<Identifier, List<String>> byScenario = new TreeMap<>(Comparator.comparing(Identifier::toString));
        roster.forEach((id, scenario) -> {
            ServerPlayer bot = source.getServer().getPlayerList().getPlayer(id);
            if (bot != null) {
                byScenario.computeIfAbsent(BotScenarios.REGISTRY.getKey(scenario), _ -> new ArrayList<>()).add(bot.getName().getString());
            }
        });

        if (byScenario.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No bots"), false);
            return 0;
        }

        byScenario.forEach((scenarioId, names) -> {
            names.sort(Comparator.naturalOrder());
            source.sendSuccess(() -> Component.literal(scenarioId + " (" + names.size() + ") - " + String.join(", ", names)), false);
        });

        return roster.size();
    }

    private static BotScenario resolve(Identifier scenarioId) throws CommandSyntaxException {
        BotScenario scenario = BotScenarios.REGISTRY.getValue(scenarioId);
        if (scenario == null) {
            throw UNKNOWN_SCENARIO.create(scenarioId);
        }

        return scenario;
    }

    private static int clear(CommandSourceStack source, int count, ClearOrder order) {
        int removed = FakePlayerManager.clear(source.getServer(), count, order);
        source.sendSuccess(() -> Component.literal("Removed " + removed + " bots"), true);
        return removed;
    }

    private static int clearWithin(CommandSourceStack source, int radius) {
        int removed = FakePlayerManager.clearWithin(source.getServer(), source.getPosition(), radius);
        source.sendSuccess(() -> Component.literal("Removed " + removed + " bots within " + radius + " blocks"), true);
        return removed;
    }

    private static int positions(CommandSourceStack source) {
        List<ServerPlayer> bots = new ArrayList<>();
        FakePlayerManager.roster().keySet().forEach(id -> {
            ServerPlayer bot = source.getServer().getPlayerList().getPlayer(id);
            if (bot != null) {
                bots.add(bot);
            }
        });

        if (bots.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No bots"), false);
            return 0;
        }

        bots.sort(Comparator.comparing(bot -> bot.getName().getString()));
        bots.forEach(bot -> source.sendSuccess(() -> Component.literal(bot.getName().getString() + " - " + bot.level().dimension().identifier() + " [" + (int) bot.getX() + ", " + (int) bot.getY() + ", " + (int) bot.getZ() + "]"), false));
        return bots.size();
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(FakePlayerManager.count() + " bots active"), false);
        return FakePlayerManager.count();
    }
}
