package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.DamagePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatModificationPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.util.TopologicalSort;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.DamagePhaseImpl;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class StatModificationPhaseImpl implements StatModificationPhase {
    public static final MutableBoolean SORTED = new MutableBoolean(false);
    public static final Comparator<StatModificationPhase> COMPARATOR = (o1, o2) -> {
        if (!SORTED.booleanValue()) {
            final List<StatModificationPhase> phases = Tbcexv4Registries.StatModificationPhases.REGISTRY.stream().toList();
            final List<StatModificationPhase> sorted = TopologicalSort.tieBreakingSort(phases, (parent, child, items) -> {
                final StatModificationPhase parentValue = items.get(parent);
                final Identifier parentId = Tbcexv4Registries.StatModificationPhases.REGISTRY.getId(parentValue);
                final StatModificationPhase childValue = items.get(child);
                final Identifier childId = Tbcexv4Registries.StatModificationPhases.REGISTRY.getId(childValue);
                return parentValue.after().contains(childId) || childValue.before().contains(parentId);
            }, (first, second, items) -> {
                final Identifier parentId = Tbcexv4Registries.StatModificationPhases.REGISTRY.getId(items.get(first));
                final Identifier childId = Tbcexv4Registries.StatModificationPhases.REGISTRY.getId(items.get(second));
                return parentId.compareTo(childId);
            });
            final int size = sorted.size();
            for (int i = 0; i < size; i++) {
                ((StatModificationPhaseImpl) sorted.get(i)).ordinal = i;
            }
            SORTED.setTrue();
        }
        return Integer.compare(((StatModificationPhaseImpl) o1).ordinal, ((StatModificationPhaseImpl) o2).ordinal);
    };
    private final Set<Identifier> dependencies;
    private final Set<Identifier> dependents;
    private int ordinal = -1;

    public StatModificationPhaseImpl(final Set<Identifier> dependencies, final Set<Identifier> dependents) {
        this.dependencies = Set.copyOf(dependencies);
        this.dependents = Set.copyOf(dependents);
    }

    @Override
    public Set<Identifier> before() {
        return dependencies;
    }

    @Override
    public Set<Identifier> after() {
        return dependents;
    }
}
