package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayDeque;
import java.util.Queue;

final class DirectLink implements BotLink {
    private static final Component END_OF_STREAM = Component.translatable("disconnect.endOfStream");
    private final FakeConnection connection;
    private final Queue<Packet<?>> held = new ArrayDeque<>();

    DirectLink(FakeConnection connection) {
        this.connection = connection;
    }

    @Override
    public void send(Packet<?> packet) {
        held.add(packet);
        while (!connection.swapping() && !held.isEmpty()) {
            connection.receive(held.poll());
        }
    }

    @Override
    public void switchOutbound(ProtocolInfo<?> protocol) {
    }

    @Override
    public void compress(int threshold) {
    }

    @Override
    public boolean open() {
        return connection.isConnected();
    }

    @Override
    public void close() {
        connection.disconnect(END_OF_STREAM);
    }
}
