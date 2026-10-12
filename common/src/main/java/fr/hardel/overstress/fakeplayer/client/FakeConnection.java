package fr.hardel.overstress.fakeplayer.client;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

import java.net.SocketAddress;

final class FakeConnection extends Connection {
    private final ClientInbox inbox;
    private final SocketAddress address;
    private volatile boolean swapping;

    FakeConnection(ClientInbox inbox, SocketAddress address) {
        super(PacketFlow.SERVERBOUND);
        this.inbox = inbox;
        this.address = address;
    }

    boolean swapping() {
        return swapping;
    }

    void receive(Packet<?> packet) {
        swapping = packet.isTerminal();
        ChannelHandlerContext context = this.channel.pipeline().context(this);
        try {
            channelRead0(context, packet);
        } catch (RuntimeException exception) {
            exceptionCaught(context, exception);
        }
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return address;
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocol, T listener) {
        this.packetListener = listener;
        swapping = false;
    }

    @Override
    public void setupOutboundProtocol(ProtocolInfo<?> protocol) {
    }

    @Override
    public void setupCompression(int threshold, boolean validateDecompressed) {
    }

    @Override
    public void send(Packet<?> packet) {
        inbox.deliver(packet);
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener) {
        send(packet, listener, true);
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
        inbox.deliver(packet);
        if (listener != null) {
            complete(listener);
        }
    }

    @Override
    public void flushChannel() {
    }

    private void complete(ChannelFutureListener listener) {
        try {
            listener.operationComplete(this.channel.newSucceededFuture());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
