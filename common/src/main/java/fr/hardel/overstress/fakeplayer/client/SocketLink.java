package fr.hardel.overstress.fakeplayer.client;

import io.netty.channel.Channel;
import net.minecraft.network.CompressionEncoder;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.UnconfiguredPipelineHandler;
import net.minecraft.network.protocol.Packet;

final class SocketLink implements BotLink {
    private final Channel channel;

    SocketLink(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(Packet<?> packet) {
        channel.writeAndFlush(packet);
    }

    @Override
    public void switchOutbound(ProtocolInfo<?> protocol) {
        channel.writeAndFlush(UnconfiguredPipelineHandler.setupOutboundProtocol(protocol));
    }

    @Override
    public void compress(int threshold) {
        channel.pipeline().addAfter("prepender", "compress", new CompressionEncoder(threshold));
    }

    @Override
    public boolean open() {
        return channel.isActive();
    }

    @Override
    public void close() {
        channel.close();
    }
}
