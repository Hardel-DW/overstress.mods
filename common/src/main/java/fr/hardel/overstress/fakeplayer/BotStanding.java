package fr.hardel.overstress.fakeplayer;

import net.minecraft.world.level.GameType;

public record BotStanding(GameType gameMode, boolean invulnerable) {
    public static final BotStanding PLAYER = new BotStanding(GameType.SURVIVAL, true);
}
