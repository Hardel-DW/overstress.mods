package fr.hardel.overstress.fakeplayer.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class ClientEntities {
    private final Int2ObjectMap<SeenEntity> seen = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, GameType> gameModes = new HashMap<>();

    public boolean hurtable(SeenEntity entity) {
        if (entity.type() == EntityTypes.PLAYER) {
            return gameModes.get(entity.uuid()) == GameType.SURVIVAL;
        }

        return entity.type().getCategory() != MobCategory.MISC;
    }

    public @Nullable SeenEntity nearest(Vec3 from, AABB within, Predicate<SeenEntity> filter) {
        SeenEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (SeenEntity entity : seen.values()) {
            double distance = entity.position().distanceToSqr(from);
            if (distance < best && entity.alive() && within.contains(entity.position()) && filter.test(entity)) {
                nearest = entity;
                best = distance;
            }
        }

        return nearest;
    }

    public int count(AABB within, Predicate<SeenEntity> filter) {
        int count = 0;
        for (SeenEntity entity : seen.values()) {
            if (entity.alive() && within.contains(entity.position()) && filter.test(entity)) {
                count++;
            }
        }

        return count;
    }

    void clear() {
        seen.clear();
    }

    void add(ClientboundAddEntityPacket packet) {
        seen.put(packet.getId(), new SeenEntity(packet.getId(), packet.getUUID(), packet.getType(), new Vec3(packet.getX(), packet.getY(), packet.getZ())));
    }

    void players(ClientboundPlayerInfoUpdatePacket packet) {
        if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) {
            return;
        }

        packet.entries().forEach(entry -> gameModes.put(entry.profileId(), entry.gameMode()));
    }

    void players(ClientboundPlayerInfoRemovePacket packet) {
        packet.profileIds().forEach(gameModes::remove);
    }

    void move(ClientboundMoveEntityPacket packet) {
        SeenEntity entity = seen.get(packet.entityId);
        if (entity != null && packet.hasPosition()) {
            Vec3 position = packet.getPositionDelta().decode(entity.codec()).endPosition();
            entity.codec().setBase(position);
            entity.moveTo(position);
        }
    }

    void sync(ClientboundEntityPositionSyncPacket packet) {
        SeenEntity entity = seen.get(packet.id());
        if (entity != null) {
            Vec3 position = packet.position().endPosition();
            entity.codec().setBase(position);
            entity.moveTo(position);
        }
    }

    void teleport(ClientboundTeleportEntityPacket packet) {
        SeenEntity entity = seen.get(packet.id());
        if (entity != null) {
            PositionMoveRotation current = new PositionMoveRotation(entity.position(), Vec3.ZERO, 0, 0);
            entity.moveTo(PositionMoveRotation.calculateAbsolute(current, packet.change(), packet.relatives()).position());
        }
    }

    void remove(ClientboundRemoveEntitiesPacket packet) {
        IntList ids = packet.entityIds();
        for (int index = 0; index < ids.size(); index++) {
            seen.remove(ids.getInt(index));
        }
    }

    void event(ClientboundEntityEventPacket packet) {
        SeenEntity entity = seen.get(packet.entityId);
        if (entity != null && packet.getEventId() == EntityEvent.DEATH) {
            entity.die();
        }
    }
}
