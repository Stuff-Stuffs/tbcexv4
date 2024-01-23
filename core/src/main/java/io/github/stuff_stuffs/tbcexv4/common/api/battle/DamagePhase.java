package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.DamagePhaseImpl;
import net.minecraft.util.Identifier;

import java.util.Set;

public interface DamagePhase {
    Set<Identifier> before();

    Set<Identifier> after();

    static DamagePhase create(final Set<Identifier> before, final Set<Identifier> after) {
        return new DamagePhaseImpl(before, after);
    }
}
