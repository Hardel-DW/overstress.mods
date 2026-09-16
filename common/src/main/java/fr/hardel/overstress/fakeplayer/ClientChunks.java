package fr.hardel.overstress.fakeplayer;

import java.util.Set;

public record ClientChunks(Set<Long> held, long received) {}
