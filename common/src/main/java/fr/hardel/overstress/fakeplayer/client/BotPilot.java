package fr.hardel.overstress.fakeplayer.client;

import net.minecraft.world.phys.Vec3;

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
    private double heading;
    private double speed;
    private int clearance;

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

    public void fly(double heading, double speed, int clearance) {
        this.flying = true;
        this.heading = heading;
        this.speed = speed;
        this.clearance = clearance;
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

    public double x() {
        return body.x();
    }

    public double z() {
        return body.z();
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
        speed = 0;
    }

    boolean flying() {
        return flying;
    }

    boolean looking() {
        return looking;
    }

    double heading() {
        return heading;
    }

    double speed() {
        return speed;
    }

    int clearance() {
        return clearance;
    }
}
