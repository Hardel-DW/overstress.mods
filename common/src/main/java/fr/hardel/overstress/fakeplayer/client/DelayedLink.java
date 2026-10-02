package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

final class DelayedLink implements BotLink {
    private final BotLink link;
    private final Queue<Consumer<BotLink>> pending = new ConcurrentLinkedQueue<>();

    DelayedLink(BotLink link) {
        this.link = link;
    }

    void flush() {
        for (Consumer<BotLink> step = pending.poll(); step != null; step = pending.poll()) {
            step.accept(link);
        }
    }

    @Override
    public void send(Packet<?> packet) {
        pending.add(target -> target.send(packet));
    }

    @Override
    public void switchOutbound(ProtocolInfo<?> protocol) {
        pending.add(target -> target.switchOutbound(protocol));
    }

    @Override
    public void compress(int threshold) {
        pending.add(target -> target.compress(threshold));
    }

    @Override
    public boolean open() {
        return link.open();
    }

    @Override
    public void close() {
        link.close();
    }
}
