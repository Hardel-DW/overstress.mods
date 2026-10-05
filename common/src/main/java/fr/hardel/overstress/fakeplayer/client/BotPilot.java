package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class BotPilot {
    private final BotBody body;
    private final BotHands hands;
    private final ClientTerrain terrain;
    private final ClientEntities entities;
    private final ClientInventory inventory;
    private final ClientStatus status;
    private long ticks;
    private boolean flying;
    private boolean looking;
    private @Nullable Vec3 guide;
    private double heading;
    private double speed;
    private int altitude;

    BotPilot(BotBody body, BotHands hands, ClientTerrain terrain, ClientEntities entities, ClientInventory inventory, ClientStatus status) {
        this.body = body;
        this.hands = hands;
        this.terrain = terrain;
        this.entities = entities;
        this.inventory = inventory;
        this.status = status;
    }

    public void walk(double heading, double speed) {
        this.flying = false;
        this.heading = heading;
        this.speed = speed;
    }

    public void fly(double heading, double speed, int altitude) {
        this.flying = true;
        this.heading = heading;
        this.speed = speed;
        this.altitude = altitude;
    }

    public void walkTo(Vec3 target, double speed) {
        double dx = target.x - body.x();
        double dz = target.z - body.z();
        walk(Math.atan2(dz, dx), Math.min(speed, Math.sqrt(dx * dx + dz * dz)));
    }

    public void look(Vec3 point) {
        body.look(point);
        looking = true;
    }

    /** This tick the bot walks to a foothold the server chose, without its own picture of the ground. */
    public void stepTo(Vec3 foot, double speed) {
        this.guide = foot;
        this.speed = speed;
    }

    public double x() {
        return body.x();
    }

    public double z() {
        return body.z();
    }

    public boolean stuck() {
        return body.stuck();
    }

    public Vec3 position() {
        return body.position();
    }

    public Vec3 eye() {
        return body.eye();
    }

    public long ticks() {
        return ticks;
    }

    public BotHands hands() {
        return hands;
    }

    public ClientTerrain terrain() {
        return terrain;
    }

    public ClientEntities entities() {
        return entities;
    }

    public ClientInventory inventory() {
        return inventory;
    }

    public ClientStatus status() {
        return status;
    }

    void next() {
        ticks++;
        flying = false;
        looking = false;
        guide = null;
        speed = 0;
    }

    boolean flying() {
        return flying;
    }

    boolean looking() {
        return looking;
    }

    @Nullable Vec3 guide() {
        return guide;
    }

    double heading() {
        return heading;
    }

    double speed() {
        return speed;
    }

    int altitude() {
        return altitude;
    }
}
