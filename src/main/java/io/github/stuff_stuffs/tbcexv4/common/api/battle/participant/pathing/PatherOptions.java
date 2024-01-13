package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

public interface PatherOptions {
    String MAX_DEPTH_KEY = "max_depth";

    boolean getFlag(String key, boolean defaultValue);

    double getValue(String key, double defaultValue);
}
