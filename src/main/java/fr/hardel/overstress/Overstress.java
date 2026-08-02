package fr.hardel.overstress;

import fr.hardel.overstress.fakeplayer.BotScenarios;
import fr.hardel.overstress.fakeplayer.FakePlayerCommand;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Overstress implements ModInitializer {
    public static final String MOD_ID = "overstress";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        BotScenarios.bootstrap();
        FakePlayerCommand.register();
    }
}
