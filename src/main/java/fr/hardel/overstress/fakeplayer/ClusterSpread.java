package fr.hardel.overstress.fakeplayer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class ClusterSpread {
    private final RandomSource random;
    private final int percent;
    private final int radius;
    private final List<Vec3> placed;

    public ClusterSpread(RandomSource random, int percent, int radius, List<Vec3> placed) {
        this.random = random;
        this.percent = percent;
        this.radius = radius;
        this.placed = new ArrayList<>(placed);
    }

    public Vec3 next(Vec3 scattered) {
        Vec3 position = this.placed.isEmpty() || this.random.nextInt(100) >= this.percent ? scattered : besidePlaced();
        this.placed.add(position);
        return position;
    }

    private Vec3 besidePlaced() {
        return this.placed.get(this.random.nextInt(this.placed.size())).add(offset(), 0, offset());
    }

    private int offset() {
        return this.random.nextInt(this.radius * 2 + 1) - this.radius;
    }
}
