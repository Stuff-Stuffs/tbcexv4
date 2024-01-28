package io.github.stuff_stuffs.tbcexv4util.log;

public enum BattleLogLevel {
    NONE,
    NORMAL,
    INFO,
    DEBUG;

    public boolean enabled(final BattleLogLevel current) {
        return ordinal() <= current.ordinal();
    }
}
