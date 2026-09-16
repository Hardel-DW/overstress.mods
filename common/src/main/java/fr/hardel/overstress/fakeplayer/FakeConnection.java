package fr.hardel.overstress.fakeplayer;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** A connection with nothing behind it; the channel is an in-memory one, so what reads its attributes, NeoForge's network registry, finds a channel. */
final class FakeConnection extends Connection {
    private final Set<Long> heldChunks = ConcurrentHashMap.newKeySet();
    private final AtomicLong receivedChunks = new AtomicLong();
    private final AtomicInteger unacknowledgedBatches = new AtomicInteger();

    FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        this.channel = new EmbeddedChannel();
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocol, T listener) {
        this.packetListener = listener;
    }

    @Override
    public void setupOutboundProtocol(ProtocolInfo<?> protocol) {
    }

    @Override
    public void send(Packet<?> packet) {
        switch (packet) {
            case ClientboundLevelChunkWithLightPacket chunk -> {
                heldChunks.add(ChunkPos.pack(chunk.x(), chunk.z()));
                receivedChunks.incrementAndGet();
            }
            case ClientboundForgetLevelChunkPacket forget -> heldChunks.remove(forget.pos().pack());
            case ClientboundChunkBatchFinishedPacket _ -> unacknowledgedBatches.incrementAndGet();
            default -> { }
        }
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener) {
        send(packet);
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
        send(packet);
    }

    @Override
    public void flushChannel() {
    }

    void acknowledgeBatches(PlayerChunkSender sender) {
        for (int owed = unacknowledgedBatches.getAndSet(0); owed > 0; owed--) {
            sender.onChunkBatchReceivedByClient(PlayerChunkSender.MAX_CHUNKS_PER_TICK);
        }
    }

    ClientChunks clientChunks() {
        return new ClientChunks(heldChunks, receivedChunks.get());
    }
}
