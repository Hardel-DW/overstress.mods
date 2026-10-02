package fr.hardel.overstress.fakeplayer.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.Varint21LengthFieldPrepender;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;

final class ClientPipeline extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel channel) {
        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
        channel.pipeline()
            .addLast("sink", new Sink())
            .addLast("prepender", new Varint21LengthFieldPrepender())
            .addLast("encoder", new PacketEncoder<>(HandshakeProtocols.SERVERBOUND));
    }

    private static final class Sink extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) {
            ReferenceCountUtil.release(message);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            context.close();
        }
    }
}
