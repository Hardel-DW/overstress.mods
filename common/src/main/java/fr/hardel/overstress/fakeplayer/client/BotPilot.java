package fr.hardel.overstress.fakeplayer.client;

public final class BotPilot {
    private final BotBody body;
    private long ticks;
    private boolean flying;
    private double heading;
    private double speed;
    private int clearance;

    BotPilot(BotBody body) {
        this.body = body;
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

    public double x() {
        return body.x();
    }

    public double z() {
        return body.z();
    }

    public long ticks() {
        return ticks;
    }

    void next() {
        ticks++;
        flying = false;
        speed = 0;
    }

    boolean flying() {
        return flying;
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
