package fr.hardel.overstress.fakeplayer.client;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

final class BotBody {
    private static final double MAX_CLIMB_PER_TICK = 4;
    static final double STEP_HEIGHT = 1;
    private static final double GRAVITY = Attributes.GRAVITY.value().getDefaultValue();
    private static final double JUMP = Attributes.JUMP_STRENGTH.value().getDefaultValue();
    private static final double VERTICAL_DRAG = 0.98;
    private static final double SINK_OVER_UNRECEIVED_CHUNK = 0.1;
    private static final double MOVE_THRESHOLD = 2.0E-4;
    private static final int POSITION_REMINDER_TICKS = 20;
    private static final Input FORWARD = new Input(true, false, false, false, false, false, false);
    private static final double STEP_REACH = 0.5 + ClientTerrain.HALF_WIDTH;
    private static final double DROP_REACH = 0.5 - ClientTerrain.HALF_WIDTH;
    private static final double EYE_HEIGHT = EntityTypes.PLAYER.getDimensions().eyeHeight();

    private boolean placed;
    private double x;
    private double y;
    private double z;
    private double verticalSpeed;
    private float yRot;
    private float xRot;
    private boolean onGround;
    private boolean horizontalCollision;
    private boolean stuck;
    private boolean fallFlying;
    private boolean takingOff;
    private Input input = Input.EMPTY;

    private double xLast;
    private double yLast;
    private double zLast;
    private float yRotLast;
    private float xRotLast;
    private boolean lastOnGround;
    private boolean lastHorizontalCollision;
    private int positionReminder;
    private Input lastInput = Input.EMPTY;

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    double z() {
        return z;
    }

    float yRot() {
        return yRot;
    }

    float xRot() {
        return xRot;
    }

    boolean onGround() {
        return onGround;
    }

    Vec3 position() {
        return new Vec3(x, y, z);
    }

    Vec3 eye() {
        return new Vec3(x, y + EYE_HEIGHT, z);
    }

    boolean stuck() {
        return stuck;
    }

    boolean placed() {
        return placed;
    }

    void look(Vec3 point) {
        Vec3 sight = point.subtract(eye());
        yRot = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(sight.z, sight.x)) - 90);
        xRot = Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(sight.y, sight.horizontalDistance())));
    }

    void unplace() {
        placed = false;
        fallFlying = false;
        verticalSpeed = 0;
    }

    ServerboundAcceptTeleportationPacket teleport(ClientboundPlayerPositionPacket packet) {
        PositionMoveRotation current = new PositionMoveRotation(new Vec3(x, y, z), new Vec3(0, verticalSpeed, 0), yRot, xRot);
        PositionMoveRotation next = PositionMoveRotation.calculateAbsolute(current, packet.change(), packet.relatives());
        x = next.position().x;
        y = next.position().y;
        z = next.position().z;
        verticalSpeed = next.deltaMovement().y;
        yRot = next.yRot();
        xRot = next.xRot();
        placed = true;
        return new ServerboundAcceptTeleportationPacket(packet.id(), x, y, z, yRot, xRot);
    }

    void synchronize(ClientboundSetEntityDataPacket packet) {
        for (SynchedEntityData.DataValue<?> data : packet.packedItems()) {
            if (data.id() == Entity.DATA_SHARED_FLAGS_ID.id() && data.value() instanceof Byte flags) {
                fallFlying = (flags & 1 << Entity.FLAG_FALL_FLYING) != 0;
            }
        }
    }

    /** Under the surface the bot knows no floor: it stays where the server put it. */
    void travel(BotPilot pilot, ClientTerrain terrain) {
        Vec3 guide = pilot.guide();
        if (guide != null) {
            follow(guide, pilot);
            return;
        }

        OptionalInt roof = terrain.height(Mth.floor(x), Mth.floor(z));
        if (roof.isPresent() && y < roof.getAsInt() - STEP_HEIGHT) {
            verticalSpeed = 0;
            horizontalCollision = false;
            onGround = true;
            input = Input.EMPTY;
            return;
        }

        double nextX = x + Math.cos(pilot.heading()) * pilot.speed();
        double nextZ = z + Math.sin(pilot.heading()) * pilot.speed();
        OptionalInt ground = terrain.ground(nextX, nextZ);
        OptionalInt here = terrain.ground(x, z);
        boolean drop = !pilot.flying() && here.isPresent() && ground.isPresent() && ground.getAsInt() < here.getAsInt() - STEP_HEIGHT;
        double nextY = pilot.flying() ? glide(pilot, ground) : walk(here, drop ? here : ground, terrain.minY());
        boolean blocked = drop || ground.isPresent() && nextY < ground.getAsInt();
        if (!blocked) {
            x = nextX;
            z = nextZ;
        }

        y = nextY;
        horizontalCollision = blocked;
        OptionalInt support = blocked ? here : ground;
        onGround = support.isPresent() && nextY == support.getAsInt();
        if (pilot.speed() > 0) {
            stuck = blocked && onGround;
        }
        fallFlying = fallFlying && !onGround;
        input = pilot.speed() > 0 ? FORWARD : Input.EMPTY;
        if (pilot.speed() > 0 && !pilot.looking()) {
            yRot = (float) Math.toDegrees(pilot.heading()) - 90;
        }
    }

    void sendChanges(BotLink link, int entityId) {
        if (takingOff) {
            link.send(fallFlyingCommand(entityId));
            takingOff = false;
        }

        if (!input.equals(lastInput)) {
            link.send(new ServerboundPlayerInputPacket(input));
            lastInput = input;
        }

        positionReminder++;
        boolean move = Mth.lengthSquared(x - xLast, y - yLast, z - zLast) > Mth.square(MOVE_THRESHOLD) || positionReminder >= POSITION_REMINDER_TICKS;
        boolean rotate = yRot != yRotLast || xRot != xRotLast;
        Packet<?> packet = movePacket(move, rotate);
        if (packet != null) {
            link.send(packet);
        }

        if (move) {
            xLast = x;
            yLast = y;
            zLast = z;
            positionReminder = 0;
        }

        if (rotate) {
            yRotLast = yRot;
            xRotLast = xRot;
        }

        lastOnGround = onGround;
        lastHorizontalCollision = horizontalCollision;
    }

    /** The bot rises a step as its body reaches the block, and only falls once its whole body is over the lower one. */
    private void follow(Vec3 foot, BotPilot pilot) {
        double dx = foot.x - x;
        double dz = foot.z - z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double stride = Math.min(pilot.speed(), distance);
        if (stride > 0) {
            x += dx / distance * stride;
            z += dz / distance * stride;
        }

        double left = distance - stride;
        boolean falls = foot.y < y && left <= DROP_REACH;
        if (foot.y > y && left <= STEP_REACH) {
            y = foot.y;
        }

        if (falls) {
            y = Math.max(foot.y, y + verticalSpeed);
        }

        onGround = !falls || y == foot.y;
        verticalSpeed = onGround ? 0 : (verticalSpeed - GRAVITY) * VERTICAL_DRAG;
        horizontalCollision = false;
        stuck = false;
        fallFlying = false;
        input = stride > 0 ? FORWARD : Input.EMPTY;
        if (stride > 0 && !pilot.looking()) {
            yRot = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        }
    }

    private double walk(OptionalInt here, OptionalInt ahead, int minY) {
        if (ahead.isEmpty()) {
            verticalSpeed = 0;
            return Math.max(minY, y - SINK_OVER_UNRECEIVED_CHUNK);
        }

        int step = ahead.getAsInt();
        if (step > y && step <= y + STEP_HEIGHT) {
            verticalSpeed = 0;
            return step;
        }

        double next = y + verticalSpeed;
        int floor = next >= step ? step : here.orElse(step);
        if (next <= floor) {
            verticalSpeed = 0;
            return floor;
        }

        verticalSpeed = (verticalSpeed - GRAVITY) * VERTICAL_DRAG;
        return next;
    }

    private double glide(BotPilot pilot, OptionalInt ground) {
        if (!fallFlying) {
            return takeOff(ground);
        }

        return y + Mth.clamp(pilot.altitude() - y, -MAX_CLIMB_PER_TICK, MAX_CLIMB_PER_TICK);
    }

    private double takeOff(OptionalInt ground) {
        if (ground.isPresent() && y <= ground.getAsInt()) {
            verticalSpeed = JUMP;
            return y + JUMP;
        }

        fallFlying = true;
        takingOff = true;
        return y;
    }

    private @Nullable Packet<?> movePacket(boolean move, boolean rotate) {
        if (move && rotate) {
            return new ServerboundMovePlayerPacket.PosRot(new Vec3(x, y, z), yRot, xRot, onGround, horizontalCollision);
        }

        if (move) {
            return new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, horizontalCollision);
        }

        if (rotate) {
            return new ServerboundMovePlayerPacket.Rot(yRot, xRot, onGround, horizontalCollision);
        }

        if (lastOnGround != onGround || lastHorizontalCollision != horizontalCollision) {
            return new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision);
        }

        return null;
    }

    private static Packet<?> fallFlyingCommand(int entityId) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(entityId);
        buffer.writeEnum(ServerboundPlayerCommandPacket.Action.START_FALL_FLYING);
        buffer.writeVarInt(0);
        return ServerboundPlayerCommandPacket.STREAM_CODEC.decode(buffer);
    }
}
