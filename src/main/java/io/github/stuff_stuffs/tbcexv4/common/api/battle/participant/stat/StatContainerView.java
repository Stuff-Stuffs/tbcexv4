package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat;

public interface StatContainerView {
    <T> T get(Stat<T> stat);
}
