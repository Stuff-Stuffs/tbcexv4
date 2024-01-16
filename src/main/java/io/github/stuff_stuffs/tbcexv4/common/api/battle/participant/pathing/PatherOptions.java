package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

public interface PatherOptions {
    PatherOptions NONE = new PatherOptions() {
        @Override
        public boolean getFlag(final String key, final boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public double getValue(final String key, final double defaultValue) {
            return defaultValue;
        }
    };
    String MAX_DEPTH_KEY = "max_depth";


    boolean getFlag(String key, boolean defaultValue);

    double getValue(String key, double defaultValue);
}
