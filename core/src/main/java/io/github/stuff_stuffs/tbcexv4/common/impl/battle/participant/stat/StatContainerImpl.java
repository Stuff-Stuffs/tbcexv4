package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.stat;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageTypeGraph;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.*;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant.AddParticipantStateModifierTrace;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant.PreAddParticipantStateModifierTrace;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.Comparator;
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
    public <T> ModifierHandle addStatModifier(final Stat<T> stat, final Modifier<T> modifier, final StatModificationPhase phase, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        if (participant.phase() == BattleParticipantPhase.FINISHED) {
            throw new RuntimeException();
        }
        try (final var preSpan = tracer.push(new PreAddParticipantStateModifierTrace(participant.handle(), stat), transactionContext)) {
            final T oldVal = get(stat);
            //noinspection unchecked
            final ModifierHandle handle = ((SingleContainer<T>) containers.computeIfAbsent(stat, SingleContainer::new)).addModifier(modifier, phase);
            final T newVal = get(stat);
            delta(transactionContext, new Delta(handle));
            try (final var span = preSpan.push(new AddParticipantStateModifierTrace(participant.handle(), stat), transactionContext)) {
                participant.events().invoker(BasicParticipantEvents.POST_ADD_MODIFIER_EVENT_KEY, transactionContext).onPostAddModifierEvent(participant, stat, phase, oldVal, newVal, transactionContext, span);
            }
            return handle;
        }
    }

    @Override
    public <T> T get(final Stat<T> stat) {
        if (stat instanceof RegisteredStat<T>) {
            //noinspection unchecked
            return ((SingleContainer<T>) containers.computeIfAbsent(stat, SingleContainer::new)).compute(new ModificationContext() {
            });
        } else if (stat instanceof final DamageResistanceStat damageResistanceStat) {
            //noinspection unchecked
            return (T) (Double) resistanceGet(damageResistanceStat, new ModificationContext() {
            });
        }
        throw new UnsupportedOperationException();
    }

    private double resistanceGet(final DamageResistanceStat stat, final ModificationContext context) {
        double res = 0;
        final SingleContainer<?> container = containers.get(stat);
        if (container != null) {
            res = (Double) container.compute(context);
        }
        final DamageType type = stat.damageType();
        final DamageTypeGraph graph = DamageTypeGraph.get(Tbcexv4Registries.DamageTypes.REGISTRY);
        final DamageTypeGraph.Node node = graph.get(type);
        for (final DamageTypeGraph.Node parent : node.parents()) {
            final double resistanceGet = resistanceGet(DamageResistanceStat.of(parent.delegate()), context);
            if (res != 0) {
                res = res * (1 + resistanceGet);
            } else {
                res = resistanceGet;
            }
        }
        return res;
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
                final List<Entry<T>> entries = new ObjectArrayList<>(map.values());
                entries.sort(Comparator.comparing(entry -> entry.phase, Tbcexv4Registries.StatModificationPhases.COMPARATOR));
                sorted = new ArrayList<>(entries.size());
                for (final Entry<T> entry : entries) {
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
