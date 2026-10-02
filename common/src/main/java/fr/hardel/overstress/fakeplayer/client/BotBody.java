package fr.hardel.overstress.fakeplayer.client;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

final class BotBody {
    private static final double MAX_CLIMB_PER_TICK = 4;
    private static final double GRAVITY = Attributes.GRAVITY.value().getDefaultValue();
    private static final double JUMP = Attributes.JUMP_STRENGTH.value().getDefaultValue();
    private static final double VERTICAL_DRAG = 0.98;
    private static final double SINK_OVER_UNRECEIVED_CHUNK = 0.1;
    private static final double MOVE_THRESHOLD = 2.0E-4;
    private static final int POSITION_REMINDER_TICKS = 20;
    private static final Input FORWARD = new Input(true, false, false, false, false, false, false);

    private boolean placed;
    private double x;
    private double y;
    private double z;
    private double verticalSpeed;
    private float yRot;
    private float xRot;
    private boolean onGround;
    private boolean horizontalCollision;
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

    boolean placed() {
        return placed;
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

    void travel(BotPilot pilot, ClientTerrain terrain) {
        double nextX = x + Math.cos(pilot.heading()) * pilot.speed();
        double nextZ = z + Math.sin(pilot.heading()) * pilot.speed();
        OptionalInt ground = terrain.top(nextX, nextZ);
        double nextY = pilot.flying() ? glide(pilot, ground) : walk(ground, terrain.minY());
        boolean blocked = ground.isPresent() && nextY < ground.getAsInt();
        if (!blocked) {
            x = nextX;
            z = nextZ;
        }

        y = nextY;
        horizontalCollision = blocked;
        onGround = !blocked && ground.isPresent() && nextY == ground.getAsInt();
        fallFlying = fallFlying && !onGround;
        input = pilot.speed() > 0 ? FORWARD : Input.EMPTY;
        if (pilot.speed() > 0) {
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

    private double walk(OptionalInt ground, int minY) {
        if (ground.isEmpty()) {
            verticalSpeed = 0;
            return Math.max(minY, y - SINK_OVER_UNRECEIVED_CHUNK);
        }

        if (ground.getAsInt() > y) {
            verticalSpeed = 0;
            return Math.min(ground.getAsInt(), y + MAX_CLIMB_PER_TICK);
        }

        verticalSpeed = (verticalSpeed - GRAVITY) * VERTICAL_DRAG;
        double fallen = Math.max(ground.getAsInt(), y + verticalSpeed);
        verticalSpeed = fallen == ground.getAsInt() ? 0 : verticalSpeed;
        return fallen;
    }

    private double glide(BotPilot pilot, OptionalInt ground) {
        if (!fallFlying) {
            return takeOff();
        }

        double target = ground.isPresent() ? ground.getAsInt() + pilot.clearance() : y;
        return y + Mth.clamp(target - y, -MAX_CLIMB_PER_TICK, MAX_CLIMB_PER_TICK);
    }

    private double takeOff() {
        if (onGround) {
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
