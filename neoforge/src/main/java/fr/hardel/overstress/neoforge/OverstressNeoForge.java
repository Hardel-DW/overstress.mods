package fr.hardel.overstress.neoforge;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.OverstressCommand;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(Overstress.MOD_ID)
public final class OverstressNeoForge {

    public OverstressNeoForge() {
        Overstress.bootstrap();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher().register(OverstressCommand.node()));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> Overstress.onServerStarting(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> Overstress.onServerStopping(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> Overstress.onServerTick(event.getServer()));
    }
}
