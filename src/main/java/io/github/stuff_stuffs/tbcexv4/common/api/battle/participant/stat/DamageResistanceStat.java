package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.damage.DamageResistanceStatImpl;

public non-sealed interface DamageResistanceStat extends Stat<Double> {
    DamageType damageType();

    static DamageResistanceStat of(final DamageType type) {
        return new DamageResistanceStatImpl(type);
    }
}
