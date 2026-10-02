package fr.hardel.overstress.fakeplayer.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.GameProfile;
import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.BotTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class BotClients {
    private static final long TICK_MILLIS = 50;
    private static final String DIRECT_HOST = "overstress";
    private final MinecraftServer server;
    private final Consumer<BotClient> disconnected;
    private final EventLoopGroup loop = new MultiThreadIoEventLoopGroup(1, new ThreadFactoryBuilder().setNameFormat("Overstress Client #%d").setDaemon(true).build(),
        NioIoHandler.newFactory());
    private final List<BotClient> clients = new CopyOnWriteArrayList<>();
    private final Set<SocketAddress> addresses = ConcurrentHashMap.newKeySet();
    private final Map<SocketAddress, ClientboundTap> taps = new ConcurrentHashMap<>();

    public BotClients(MinecraftServer server, Consumer<BotClient> disconnected) {
        this.server = server;
        this.disconnected = disconnected;
        loop.scheduleAtFixedRate(this::tick, TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
    }

    public BotClient join(GameProfile profile, BotState state, Vec3 position, BotTransport transport) {
        BotClient client = new BotClient(profile, state, information(), server.registryAccess());
        loop.execute(() -> {
            new BotSave(position, state.yRot()).write(server, profile.id());
            connect(client, transport);
        });
        return client;
    }

    public boolean isBot(SocketAddress address) {
        return addresses.contains(address);
    }

    public void attach(Connection connection) {
        ClientboundTap tap = taps.remove(connection.getRemoteAddress());
        if (tap != null) {
            connection.channel.pipeline().addLast(ClientboundTap.NAME, tap);
        }
    }

    public void close() {
        loop.shutdownGracefully(0, 1, TimeUnit.SECONDS);
    }

    private void connect(BotClient client, BotTransport transport) {
        switch (transport) {
            case DIRECT -> connectDirect(client);
            case NETWORK -> connectSocket(client);
        }
    }

    private void connectDirect(BotClient client) {
        SocketAddress address = InetSocketAddress.createUnresolved(client.profile().name(), 0);
        FakeConnection connection = new FakeConnection(client.inbox(), address);
        new EmbeddedChannel(connection);
        connection.setListenerForServerboundHandshake(new ServerHandshakePacketListenerImpl(server, connection));
        addresses.add(address);
        server.execute(() -> {
            server.getConnection().getConnections().add(connection);
            loop.execute(() -> connected(client, new DirectLink(connection), address, DIRECT_HOST, 0));
        });
    }

    private void connectSocket(BotClient client) {
        int port = server.getPort();
        ChannelFuture connecting = new Bootstrap().group(loop).channel(NioSocketChannel.class).handler(new ClientPipeline())
            .connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        connecting.addListener(_ -> socketConnected(client, connecting, port));
    }

    private void socketConnected(BotClient client, ChannelFuture connecting, int port) {
        if (!connecting.isSuccess()) {
            Overstress.LOGGER.warn("{} could not reach the server on port {}", client.profile().name(), port, connecting.cause());
            disconnected.accept(client);
            return;
        }

        Channel channel = connecting.channel();
        SocketAddress address = channel.localAddress();
        addresses.add(address);
        taps.put(address, new ClientboundTap(client.inbox()));
        connected(client, new SocketLink(channel), address, InetAddress.getLoopbackAddress().getHostAddress(), port);
    }

    private void connected(BotClient client, BotLink link, SocketAddress address, String host, int port) {
        client.connected(link, address, host, port);
        clients.add(client);
    }

    private void tick() {
        for (BotClient client : clients) {
            if (!client.tick()) {
                clients.remove(client);
                addresses.remove(client.address());
                taps.remove(client.address());
                disconnected.accept(client);
            }
        }
    }

    private ClientInformation information() {
        ClientInformation defaults = ClientInformation.createDefault();
        return new ClientInformation(defaults.language(), server.getPlayerList().getViewDistance(), defaults.chatVisibility(), defaults.chatColors(), defaults.modelCustomisation(),
            defaults.mainHand(), defaults.textFilteringEnabled(), defaults.allowsListing(), defaults.particleStatus());
    }
}
