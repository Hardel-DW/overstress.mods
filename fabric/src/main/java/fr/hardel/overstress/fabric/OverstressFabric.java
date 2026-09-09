package fr.hardel.overstress.fabric;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.OverstressCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class OverstressFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Overstress.bootstrap();
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> dispatcher.register(OverstressCommand.node()));
        ServerTickEvents.END_SERVER_TICK.register(Overstress::onServerTick);
    }
}
