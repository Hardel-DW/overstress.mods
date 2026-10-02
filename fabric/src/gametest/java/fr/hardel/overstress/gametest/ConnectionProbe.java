package fr.hardel.overstress.gametest;

import net.minecraft.server.level.ServerPlayer;

public interface ConnectionProbe {

    static ConnectionProbe of(ServerPlayer player) {
        return (ConnectionProbe) player.connection;
    }

    void overstressTest$doTick();

    void overstressTest$act();

    void overstressTest$handling(boolean handling);

    int overstressTest$tickPlayers();

    int overstressTest$strayTicks();

    int overstressTest$actions();

    int overstressTest$strayActions();
}
