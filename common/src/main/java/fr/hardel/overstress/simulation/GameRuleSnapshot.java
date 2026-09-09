package fr.hardel.overstress.simulation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

public final class GameRuleSnapshot {
    private final MinecraftServer server;
    private final List<Frozen<?>> frozen = new ArrayList<>();

    private GameRuleSnapshot(MinecraftServer server) {
        this.server = server;
    }

    public static GameRuleSnapshot freeze(MinecraftServer server, boolean mobSpawning) {
        GameRuleSnapshot snapshot = new GameRuleSnapshot(server);
        snapshot.apply(GameRules.SPAWN_MOBS, mobSpawning);
        snapshot.apply(GameRules.SPAWN_MONSTERS, mobSpawning);
        snapshot.apply(GameRules.ADVANCE_TIME, false);
        snapshot.apply(GameRules.ADVANCE_WEATHER, false);
        snapshot.apply(GameRules.RANDOM_TICK_SPEED, 0);
        return snapshot;
    }

    public void restore() {
        GameRules rules = this.server.getGameRules();
        for (Frozen<?> value : this.frozen) {
            value.restore(rules, this.server);
        }

        this.frozen.clear();
    }

    private <T> void apply(GameRule<T> rule, T value) {
        GameRules rules = this.server.getGameRules();
        this.frozen.add(new Frozen<>(rule, rules.get(rule)));
        rules.set(rule, value, this.server);
    }

    private record Frozen<T>(GameRule<T> rule, T value) {
        private void restore(GameRules rules, MinecraftServer server) {
            rules.set(this.rule, this.value, server);
        }
    }
}
