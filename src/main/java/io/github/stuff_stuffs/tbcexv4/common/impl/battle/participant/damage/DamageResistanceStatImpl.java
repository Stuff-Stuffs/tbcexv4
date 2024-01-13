package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.damage;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.DamageResistanceStat;
import net.minecraft.text.Text;

public class DamageResistanceStatImpl implements DamageResistanceStat {
    private final DamageType type;

    public DamageResistanceStatImpl(final DamageType type) {
        this.type = type;
    }

    @Override
    public DamageType damageType() {
        return type;
    }

    @Override
    public Text displayName() {
        return type.description();
    }

    @Override
    public Double defaultValue() {
        return 0.0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final DamageResistanceStatImpl stat)) {
            return false;
        }

        return type.equals(stat.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
