package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class SeenEntity {
    private final int id;
    private final UUID uuid;
    private final EntityType<?> type;
    private final VecDeltaCodec codec = new VecDeltaCodec();
    private Vec3 position;
    private boolean dead;

    SeenEntity(int id, UUID uuid, EntityType<?> type, Vec3 position) {
        this.id = id;
        this.uuid = uuid;
        this.type = type;
        this.position = position;
        codec.setBase(position);
    }

    public int id() {
        return id;
    }

    UUID uuid() {
        return uuid;
    }

    public EntityType<?> type() {
        return type;
    }

    public Vec3 position() {
        return position;
    }

    public boolean alive() {
        return !dead;
    }

    public AABB box() {
        return type.getDimensions().makeBoundingBox(position);
    }

    VecDeltaCodec codec() {
        return codec;
    }

    void moveTo(Vec3 position) {
        this.position = position;
    }

    void die() {
        dead = true;
    }
}
