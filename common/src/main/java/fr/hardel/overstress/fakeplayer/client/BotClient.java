package fr.hardel.overstress.fakeplayer.client;

import com.mojang.authlib.GameProfile;
import fr.hardel.overstress.fakeplayer.BotStanding;
import fr.hardel.overstress.fakeplayer.BotState;
import fr.hardel.overstress.fakeplayer.ClientChunks;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.packs.repository.KnownPack;

import java.net.SocketAddress;
import java.util.List;

public final class BotClient {
    private static final String BRAND = "vanilla";
    private static final float CHUNKS_PER_TICK = 7_000_000f / 108_000f;
    private static final int LEVEL_LOAD_TIMEOUT_TICKS = 600;
    private final GameProfile profile;
    private final BotState state;
    private final ClientInformation information;
    private final RegistryAccess registries;
    private final BotStanding standing;
    private final ClientInbox inbox = new ClientInbox();
    private final ClientEntities entities = new ClientEntities();
    private final ClientInventory inventory = new ClientInventory();
    private final ClientStatus status = new ClientStatus();
    private final BotBody body = new BotBody();
    private final ClientTerrain terrain;
    private final BotHands hands;
    private final BotPilot pilot;
    private volatile DelayedLink link;
    private SocketAddress address;
    private boolean playing;
    private int entityId;
    private LevelLoad levelLoad = LevelLoad.READY;
    private int levelLoadTicks;

    BotClient(GameProfile profile, BotState state, ClientInformation information, RegistryAccess registries) {
        this.profile = profile;
        this.state = state;
        this.standing = state.scenario().standing();
        this.information = information;
        this.registries = registries;
        this.terrain = new ClientTerrain(registries);
        this.hands = new BotHands(this::send, body, terrain, inventory, registries);
        this.pilot = new BotPilot(body, hands, terrain, entities, inventory, status);
    }

    public GameProfile profile() {
        return profile;
    }

    public BotState state() {
        return state;
    }

    public BotStanding standing() {
        return standing;
    }

    public ClientChunks chunks() {
        return inbox.chunks();
    }

    public void leave() {
        BotLink current = link;
        if (current != null) {
            current.close();
        }
    }

    ClientInbox inbox() {
        return inbox;
    }

    SocketAddress address() {
        return address;
    }

    void connected(BotLink link, SocketAddress address, String host, int port) {
        this.link = new DelayedLink(link);
        this.address = address;
        this.link.send(new ClientIntentionPacket(SharedConstants.getCurrentVersion().protocolVersion(), host, port, ClientIntent.LOGIN));
        this.link.switchOutbound(LoginProtocols.SERVERBOUND);
        this.link.send(new ServerboundHelloPacket(profile.name(), profile.id()));
    }

    boolean tick() {
        if (!link.open()) {
            return false;
        }

        link.flush();
        for (Packet<?> packet = inbox.poll(); packet != null; packet = inbox.poll()) {
            handle(packet);
        }

        if (!playing) {
            return true;
        }

        boolean loaded = levelLoad == LevelLoad.READY;
        tickLevelLoad();
        if (loaded && body.placed()) {
            hands.begin();
            pilot.next();
            state.scenario().steer(pilot, state, state.random);
            hands.end();
            body.travel(pilot, terrain);
            body.sendChanges(link, entityId);
        }

        link.send(ServerboundClientTickEndPacket.INSTANCE);
        return true;
    }

    private void handle(Packet<?> packet) {
        switch (packet) {
            case ClientboundBundlePacket bundle -> bundle.subPackets().forEach(this::handle);
            case ClientboundLoginCompressionPacket compression -> link.compress(compression.getCompressionThreshold());
            case ClientboundCustomQueryPacket query -> link.send(new ServerboundCustomQueryAnswerPacket(query.transactionId(), null));
            case ClientboundCookieRequestPacket cookie -> link.send(new ServerboundCookieResponsePacket(cookie.key(), null));
            case ClientboundLoginFinishedPacket _ -> configure(ServerboundLoginAcknowledgedPacket.INSTANCE);
            case ClientboundStartConfigurationPacket _ -> configure(ServerboundConfigurationAcknowledgedPacket.INSTANCE);
            case ClientboundSelectKnownPacks offer -> link.send(new ServerboundSelectKnownPacks(offer.knownPacks().stream().filter(KnownPack::isVanilla).toList()));
            case ClientboundCodeOfConductPacket _ -> link.send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
            case ClientboundResourcePackPushPacket pack -> link.send(new ServerboundResourcePackPacket(pack.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
            case ClientboundFinishConfigurationPacket _ -> play();
            case ClientboundKeepAlivePacket keepAlive -> link.send(new ServerboundKeepAlivePacket(keepAlive.getId()));
            case ClientboundPingPacket ping -> link.send(new ServerboundPongPacket(ping.getId()));
            case ClientboundLoginPacket login -> login(login);
            case ClientboundRespawnPacket respawn -> enter(entityId, respawn.commonPlayerSpawnInfo());
            case ClientboundGameEventPacket event when event.getEvent() == ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START -> levelLoad = LevelLoad.WAITING_FOR_CHUNK;
            case ClientboundGameEventPacket event when event.getEvent() == ClientboundGameEventPacket.WIN_GAME ->
                link.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            case ClientboundPlayerPositionPacket position -> teleport(position);
            case ClientboundLevelChunkWithLightPacket chunk -> terrain.receive(chunk);
            case ClientboundForgetLevelChunkPacket forget -> terrain.forget(forget.pos());
            case ClientboundBlockUpdatePacket update -> terrain.update(update.getPos(), update.getBlockState());
            case ClientboundSectionBlocksUpdatePacket updates -> terrain.update(updates);
            case ClientboundChunkBatchFinishedPacket _ -> link.send(new ServerboundChunkBatchReceivedPacket(CHUNKS_PER_TICK));
            case ClientboundAddEntityPacket added -> entities.add(added);
            case ClientboundPlayerInfoUpdatePacket players -> entities.players(players);
            case ClientboundPlayerInfoRemovePacket players -> entities.players(players);
            case ClientboundMoveEntityPacket move -> entities.move(move);
            case ClientboundEntityPositionSyncPacket sync -> entities.sync(sync);
            case ClientboundTeleportEntityPacket teleport -> entities.teleport(teleport);
            case ClientboundRemoveEntitiesPacket removed -> entities.remove(removed);
            case ClientboundEntityEventPacket event -> entities.event(event);
            case ClientboundDamageEventPacket damage -> entities.damage(damage);
            case ClientboundContainerSetContentPacket content -> inventory.content(content);
            case ClientboundContainerSetSlotPacket slot -> inventory.slot(slot);
            case ClientboundSetCursorItemPacket cursor -> inventory.cursor(cursor);
            case ClientboundSetHeldSlotPacket held -> inventory.held(held);
            case ClientboundSetHealthPacket health -> health(health);
            case ClientboundSetEntityDataPacket data when data.id() == entityId -> body.synchronize(data);
            default -> { }
        }
    }

    private void configure(Packet<?> acknowledgement) {
        playing = false;
        link.send(acknowledgement);
        link.switchOutbound(ConfigurationProtocols.SERVERBOUND);
        link.send(new ServerboundCustomPayloadPacket(new BrandPayload(BRAND)));
        link.send(new ServerboundClientInformationPacket(information));
    }

    private void play() {
        link.send(ServerboundFinishConfigurationPacket.INSTANCE);
        link.switchOutbound(GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries), new BotGameContext()));
        playing = true;
    }

    private void send(Packet<?> packet) {
        link.send(packet);
    }

    private void login(ClientboundLoginPacket login) {
        status.login(List.copyOf(login.levels()));
        enter(login.playerId(), login.commonPlayerSpawnInfo());
    }

    private void health(ClientboundSetHealthPacket health) {
        if (status.update(health)) {
            link.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        }
    }

    private void teleport(ClientboundPlayerPositionPacket position) {
        link.send(body.teleport(position));
        terrain.land(body.position());
        hands.stopDestroying();
    }

    private void enter(int entityId, CommonPlayerSpawnInfo spawn) {
        this.entityId = entityId;
        terrain.enter(spawn.dimensionType().value());
        entities.clear();
        status.enter(spawn.dimension());
        hands.forget();
        body.unplace();
        levelLoad = LevelLoad.WAITING_FOR_SERVER;
        levelLoadTicks = 0;
    }

    private void tickLevelLoad() {
        if (levelLoad == LevelLoad.READY) {
            return;
        }

        levelLoadTicks++;
        boolean chunkReady = levelLoad == LevelLoad.WAITING_FOR_CHUNK && body.placed() && (terrain.holds(body.x(), body.z()) || cameraAboveBuildHeight());
        if (chunkReady || levelLoadTicks > LEVEL_LOAD_TIMEOUT_TICKS) {
            link.send(new ServerboundPlayerLoadedPacket());
            levelLoad = LevelLoad.READY;
        }
    }

    private boolean cameraAboveBuildHeight() {
        return terrain.aboveBuildHeight(body.eye().y);
    }

    private enum LevelLoad {
        WAITING_FOR_SERVER,
        WAITING_FOR_CHUNK,
        READY
    }

    private record BotGameContext() implements GameProtocols.Context {

        @Override
        public boolean hasInfiniteMaterials() {
            return false;
        }

        @Override
        public boolean canUseCommandBlocks() {
            return false;
        }
    }
}
