package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat;

import net.minecraft.text.Text;

public sealed interface Stat<T> permits DamageResistanceStat, RegisteredStat {
    Text displayName();

    T defaultValue();
}
