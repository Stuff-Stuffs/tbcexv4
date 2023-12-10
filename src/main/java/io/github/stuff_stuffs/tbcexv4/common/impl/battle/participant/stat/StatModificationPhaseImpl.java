package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatModificationPhase;
import net.minecraft.util.Identifier;

import java.util.Set;

public class StatModificationPhaseImpl implements StatModificationPhase {
    private final Set<Identifier> dependencies;
    private final Set<Identifier> dependents;

    public StatModificationPhaseImpl(final Set<Identifier> dependencies, final Set<Identifier> dependents) {
        this.dependencies = Set.copyOf(dependencies);
        this.dependents = Set.copyOf(dependents);
    }

    @Override
    public Set<Identifier> dependencies() {
        return dependencies;
    }

    @Override
    public Set<Identifier> dependents() {
        return dependents;
    }
}
