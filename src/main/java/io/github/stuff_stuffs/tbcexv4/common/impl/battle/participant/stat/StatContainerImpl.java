package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatContainer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatModificationPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.util.TopologicalSort;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatContainerImpl extends DeltaSnapshotParticipant<StatContainerImpl.Delta> implements StatContainer {
    private final BattleParticipant participant;
    private final Map<Stat<?>, SingleContainer<?>> containers;

    public StatContainerImpl(final BattleParticipant participant) {
        this.participant = participant;
        containers = new Object2ReferenceOpenHashMap<>();
    }

    @Override
    public <T> ModifierHandle addStateModifier(final Stat<T> stat, final Modifier<T> modifier, final StatModificationPhase phase, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        if (participant.phase() == BattleParticipantPhase.FINISHED) {
            throw new RuntimeException();
        }
        final T oldVal = get(stat);
        //noinspection unchecked
        final ModifierHandle handle = ((SingleContainer<T>) containers.computeIfAbsent(stat, SingleContainer::new)).addModifier(modifier, phase);
        final T newVal = get(stat);
        delta(transactionContext, new Delta(handle));
        try (final var span = tracer.push(new CoreBattleTraceEvents.AddParticipantStateModifier(participant.handle(), stat), transactionContext)) {
            participant.events().invoker(BasicParticipantEvents.POST_ADD_MODIFIER_EVENT_KEY).onPostAddModifierEvent(participant, stat, oldVal, newVal, transactionContext, span);
        }
        return handle;
    }

    @Override
    public <T> T get(final Stat<T> stat) {
        //noinspection unchecked
        return ((SingleContainer<T>) containers.computeIfAbsent(stat, SingleContainer::new)).compute(new ModificationContext() {
        });
    }

    @Override
    protected void revertDelta(final Delta delta) {
        if (delta.handle.alive()) {
            delta.handle.kill();
        }
    }

    private static final class SingleContainer<T> {
        private final Stat<T> stat;
        private final Int2ObjectLinkedOpenHashMap<Entry<T>> map;
        private int nextId = 0;
        private List<Modifier<T>> sorted = null;

        private SingleContainer(final Stat<T> stat) {
            this.stat = stat;
            map = new Int2ObjectLinkedOpenHashMap<>();
        }

        public ModifierHandle addModifier(final Modifier<T> modifier, final StatModificationPhase phase) {
            final int id = nextId++;
            map.putAndMoveToLast(id, new Entry<>(modifier, phase));
            sorted = null;
            return new HandleImpl(this, id);
        }

        public T compute(final ModificationContext context) {
            if (sorted == null) {
                final List<Entry<T>> list = TopologicalSort.tieBreakingSort(new ArrayList<>(map.values()), (parent, child, items) -> {
                    final Entry<T> parentItem = items.get(parent);
                    final Entry<T> childItem = items.get(child);
                    final Identifier parentId = Tbcexv4Registries.StatModificationPhases.REGISTRY.getId(parentItem.phase);
                    if (parentId == null) {
                        throw new RuntimeException("Encountered not registered StatModificationPhase");
                    }
                    final Identifier childId = Tbcexv4Registries.StatModificationPhases.REGISTRY.getId(childItem.phase);
                    if (childId == null) {
                        throw new RuntimeException("Encountered not registered StatModificationPhase");
                    }
                    if (parentItem.phase.dependents().contains(childId) || childItem.phase.dependencies().contains(parentId)) {
                        return true;
                    }
                    return parentId.compareTo(childId) < 0;
                }, (first, second, items) -> Integer.compare(first, second));
                sorted = new ArrayList<>(list.size());
                for (final Entry<T> entry : list) {
                    sorted.add(entry.modifier);
                }
            }
            T val = stat.defaultValue();
            for (final Modifier<T> modifier : sorted) {
                val = modifier.compute(val, context);
            }
            return val;
        }

        public void kill(final int id) {
            if (map.remove(id) != null) {
                sorted = null;
            }
        }
    }

    private record HandleImpl(SingleContainer<?> container, int id) implements ModifierHandle {
        @Override
        public boolean alive() {
            return container.map.containsKey(id);
        }

        @Override
        public void kill() {
            container.kill(id);
        }
    }

    private record Entry<T>(Modifier<T> modifier, StatModificationPhase phase) {
    }

    public record Delta(ModifierHandle handle) {
    }
}
