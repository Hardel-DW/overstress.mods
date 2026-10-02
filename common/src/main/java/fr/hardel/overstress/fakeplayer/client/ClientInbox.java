package fr.hardel.overstress.fakeplayer.client;

import fr.hardel.overstress.fakeplayer.ClientChunks;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

final class ClientInbox {
    private final Queue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private final Set<Long> heldChunks = ConcurrentHashMap.newKeySet();
    private final AtomicLong receivedChunks = new AtomicLong();

    void deliver(Packet<?> packet) {
        switch (packet) {
            case ClientboundLevelChunkWithLightPacket chunk -> {
                heldChunks.add(ChunkPos.pack(chunk.x(), chunk.z()));
                receivedChunks.incrementAndGet();
            }
            case ClientboundForgetLevelChunkPacket forget -> heldChunks.remove(forget.pos().pack());
            default -> { }
        }

        packets.add(packet);
    }

    Packet<?> poll() {
        return packets.poll();
    }

    ClientChunks chunks() {
        return new ClientChunks(heldChunks, receivedChunks.get());
    }
}
