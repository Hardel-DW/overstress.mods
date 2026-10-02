package fr.hardel.overstress.fakeplayer.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.Packet;

final class ClientboundTap extends ChannelOutboundHandlerAdapter {
    static final String NAME = "overstress_tap";
    private final ClientInbox inbox;

    ClientboundTap(ClientInbox inbox) {
        this.inbox = inbox;
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        if (message instanceof Packet<?> packet) {
            inbox.deliver(packet);
        }

        context.write(message, promise);
    }
}
