package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;

interface BotLink {

    void send(Packet<?> packet);

    void switchOutbound(ProtocolInfo<?> protocol);

    void compress(int threshold);

    boolean open();

    void close();
}
