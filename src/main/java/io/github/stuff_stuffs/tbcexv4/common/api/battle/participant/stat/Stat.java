package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat;

import net.minecraft.text.Text;

public interface Stat<T> {
    Text displayName();

    T defaultValue();
}
