package fr.hardel.overstress.simulation;

import fr.hardel.overstress.Overstress;
import fr.hardel.overstress.fakeplayer.ClearOrder;
import fr.hardel.overstress.fakeplayer.ClusterSpread;
import fr.hardel.overstress.fakeplayer.FakePlayerManager;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class SimulationRunner {
    private static volatile @Nullable SimulationRunner active;

    private final MinecraftServer server;
    private final Identifier id;
    private final Simulation simulation;
    private final GameRuleSnapshot rules;
    private final long startTick;
    private final ClusterSpread cluster;
    private int spawned;

    private SimulationRunner(MinecraftServer server, Identifier id, Simulation simulation, GameRuleSnapshot rules) {
        this.server = server;
        this.id = id;
        this.simulation = simulation;
        this.rules = rules;
        this.startTick = server.getTickCount();
        this.cluster = new ClusterSpread(RandomSource.create(seedOf(simulation.bots())), simulation.clusterPercent(), simulation.clusterRadius(), List.of());
    }

    public static boolean start(MinecraftServer server, Identifier id, Simulation simulation) {
        if (active != null) {
            return false;
        }

        FakePlayerManager.clear(server, Integer.MAX_VALUE, ClearOrder.LAST);
        SimulationRunner runner = new SimulationRunner(server, id, simulation, GameRuleSnapshot.freeze(server, simulation.mobSpawning()));
        active = runner;
        Overstress.LOGGER.info("Simulation {} started at tick {}, {} bots on a ring of {} blocks, one every {} ticks, for {} ticks", id, runner.startTick,
            simulation.bots(), simulation.radius(), simulation.spawnIntervalTicks(), simulation.durationTicks());
        return true;
    }

    public static boolean stop() {
        SimulationRunner runner = active;
        if (runner == null) {
            return false;
        }

        active = null;
        runner.tearDown();
        return true;
    }

    public static void tick(MinecraftServer server) {
        SimulationRunner runner = active;
        if (runner == null) {
            return;
        }

        runner.spawnDueBots();
        if (runner.elapsedTicks() >= runner.simulation.durationTicks()) {
            stop();
        }
    }

    public static @Nullable SimulationRunner active() {
        return active;
    }

    public Identifier id() {
        return this.id;
    }

    public Simulation simulation() {
        return this.simulation;
    }

    public long elapsedTicks() {
        return this.server.getTickCount() - this.startTick;
    }

    private void spawnDueBots() {
        int interval = this.simulation.spawnIntervalTicks();
        int due = interval <= 0 ? this.simulation.bots() : (int) Math.min(this.simulation.bots(), elapsedTicks() / interval + 1);
        while (this.spawned < due) {
            int index = this.spawned++;
            double angle = 2 * Math.PI * index / this.simulation.bots();
            Vec3 ring = new Vec3(Math.cos(angle) * this.simulation.radius(), 0, Math.sin(angle) * this.simulation.radius());
            FakePlayerManager.spawn(this.server, "Sim_" + index, this.cluster.next(ring), this.simulation.scenario(), seedOf(index));
        }
    }

    private long seedOf(int index) {
        return this.id.toString().hashCode() * 31L + index;
    }

    private void tearDown() {
        FakePlayerManager.clear(this.server, Integer.MAX_VALUE, ClearOrder.LAST);
        this.rules.restore();
        Overstress.LOGGER.info("Simulation {} stopped after {} ticks", this.id, elapsedTicks());
    }
}
