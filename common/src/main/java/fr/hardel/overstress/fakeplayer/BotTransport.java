package fr.hardel.overstress.fakeplayer;

import net.minecraft.util.StringRepresentable;

public enum BotTransport implements StringRepresentable {
    DIRECT("direct"),
    NETWORK("network");

    private final String name;

    BotTransport(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
