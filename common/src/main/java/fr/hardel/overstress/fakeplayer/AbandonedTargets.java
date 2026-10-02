package fr.hardel.overstress.fakeplayer;

import java.util.Arrays;

public final class AbandonedTargets {
    private static final int SIZE = 16;
    private static final long MEMORY_TICKS = 600;
    private final int[] ids = new int[SIZE];
    private final long[] until = new long[SIZE];
    private int next;

    AbandonedTargets() {
        Arrays.fill(ids, -1);
    }

    public void add(int id, long now) {
        ids[next] = id;
        until[next] = now + MEMORY_TICKS;
        next = (next + 1) % SIZE;
    }

    public boolean contains(int id, long now) {
        for (int slot = 0; slot < SIZE; slot++) {
            if (ids[slot] == id && until[slot] > now) {
                return true;
            }
        }

        return false;
    }
}
