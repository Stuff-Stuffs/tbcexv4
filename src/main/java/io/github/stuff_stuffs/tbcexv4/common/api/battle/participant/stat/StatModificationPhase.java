package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat;

import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat.StatModificationPhaseImpl;
import net.minecraft.util.Identifier;

import java.util.Set;

public interface StatModificationPhase {
    Set<Identifier> dependencies();

    Set<Identifier> dependents();

    static StatModificationPhase create(final Set<Identifier> dependencies, final Set<Identifier> dependents) {
        return new StatModificationPhaseImpl(dependencies, dependents);
    }
}
