package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.DamagePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.util.TopologicalSort;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DamagePhaseImpl implements DamagePhase {
    public static final AtomicBoolean SORTED = new AtomicBoolean(false);
    private static final Object SORT_LOCK = new Object();
    public static final Comparator<DamagePhase> COMPARATOR = (o1, o2) -> {
        if (!SORTED.getAcquire()) {
            synchronized (SORT_LOCK) {
                if (!SORTED.getAcquire()) {
                    final List<DamagePhase> phases = Tbcexv4Registries.DamagePhases.REGISTRY.stream().toList();
                    final List<DamagePhase> sorted = TopologicalSort.tieBreakingSort(phases, (parent, child, items) -> {
                        final DamagePhase parentValue = items.get(parent);
                        final Identifier parentId = Tbcexv4Registries.DamagePhases.REGISTRY.getId(parentValue);
                        final DamagePhase childValue = items.get(child);
                        final Identifier childId = Tbcexv4Registries.DamagePhases.REGISTRY.getId(childValue);
                        return parentValue.after().contains(childId) || childValue.before().contains(parentId);
                    }, (first, second, items) -> {
                        final Identifier parentId = Tbcexv4Registries.DamagePhases.REGISTRY.getId(items.get(second));
                        final Identifier childId = Tbcexv4Registries.DamagePhases.REGISTRY.getId(items.get(second));
                        return parentId.compareTo(childId);
                    });
                    final int size = sorted.size();
                    for (int i = 0; i < size; i++) {
                        ((DamagePhaseImpl) sorted.get(i)).ordinal = i;
                    }
                    //fixme bugs be here!
                    SORTED.setRelease(true);
                }
            }
        }
        return Integer.compare(((DamagePhaseImpl) o1).ordinal, ((DamagePhaseImpl) o2).ordinal);
    };
    private final Set<Identifier> before;
    private final Set<Identifier> after;
    public int ordinal = -1;

    public DamagePhaseImpl(final Set<Identifier> before, final Set<Identifier> after) {
        this.before = Set.copyOf(before);
        this.after = Set.copyOf(after);
    }

    @Override
    public Set<Identifier> before() {
        return before;
    }

    @Override
    public Set<Identifier> after() {
        return after;
    }
}
