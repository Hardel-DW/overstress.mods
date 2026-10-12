package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ClientStatus {
    private List<ResourceKey<Level>> levels = List.of();
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private float health;
    private int food;
    private int deaths;
    private boolean instabuild;

    public List<ResourceKey<Level>> levels() {
        return levels;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public float health() {
        return health;
    }

    public int food() {
        return food;
    }

    public int deaths() {
        return deaths;
    }

    /** True in creative: a click breaks the block at once. */
    public boolean instabuild() {
        return instabuild;
    }

    void login(List<ResourceKey<Level>> levels) {
        this.levels = levels;
    }

    void enter(ResourceKey<Level> dimension) {
        this.dimension = dimension;
    }

    void abilities(ClientboundPlayerAbilitiesPacket packet) {
        instabuild = packet.canInstabuild();
    }

    boolean update(ClientboundSetHealthPacket packet) {
        boolean dies = health > 0 && packet.getHealth() <= 0;
        health = packet.getHealth();
        food = packet.getFood();
        deaths += dies ? 1 : 0;
        return packet.getHealth() <= 0;
    }
}
