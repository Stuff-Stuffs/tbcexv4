package io.github.stuff_stuffs.tbcexv4.common.impl.event;

import io.github.stuff_stuffs.event_gen.api.event.EventKey;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventMapImpl implements EventMap {
    private final Map<EventKey<?, ?>, Entry<?, ?>> eventEntries;

    EventMapImpl(final Map<EventKey<?, ?>, Entry<?, ?>> entries) {
        eventEntries = entries;
    }

    @Override
    public <Mut> Token registerMut(final EventKey<Mut, ?> key, final Mut event, final BattleTransactionContext transactionContext) {
        if (!contains(key)) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return ((Entry<Mut, ?>) eventEntries.get(key)).register(event, transactionContext);
    }

    @Override
    public <Mut> Mut invoker(final EventKey<Mut, ?> key, final BattleTransactionContext context) {
        if (!contains(key)) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return ((Entry<Mut, ?>) eventEntries.get(key)).invoker(context);
    }

    @Override
    public <View> Token registerView(final EventKey<?, View> key, final View view, final BattleTransactionContext transactionContext) {
        if (!contains(key)) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return ((Entry<?, View>) eventEntries.get(key)).registerView(view, transactionContext);
    }

    @Override
    public <View> Token registerTerminalView(final EventKey<?, View> key, final View view) {
        if (!contains(key)) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return ((Entry<?, View>) eventEntries.get(key)).registerTerminalView(view);
    }

    @Override
    public boolean contains(final EventKey<?, ?> key) {
        return eventEntries.containsKey(key);
    }

    static final class Entry<Mut, View> extends DeltaSnapshotParticipant<Delta<Mut>> {
        private final EventKey<Mut, View> key;
        private final EventKey.Factory<Mut, View> factory;
        private final ReferenceLinkedOpenHashSet<Mut> terminals;
        private ReferenceLinkedOpenHashSet<Mut> events;
        private Mut mutInvoker;
        private Mut terminalInvoker;
        private Mut dualInvoker;

        Entry(final EventKey<Mut, View> key, final EventKey.Factory<Mut, View> factory) {
            this.key = key;
            this.factory = factory;
            events = new ReferenceLinkedOpenHashSet<>();
            terminals = new ReferenceLinkedOpenHashSet<>();
        }


        public Token registerView(final View view, final BattleTransactionContext transactionContext) {
            return register(factory.convert(view), transactionContext);
        }

        public Token register(final Mut mut, final BattleTransactionContext transactionContext) {
            final ReferenceLinkedOpenHashSet<Mut> s = events;
            events = new ReferenceLinkedOpenHashSet<>(s);
            events.addAndMoveToLast(mut);
            mutInvoker = null;
            dualInvoker = null;
            delta(transactionContext, new Add<>(s));
            return new TokenImpl(mut, false, this);
        }

        public Mut invoker(final BattleTransactionContext context) {
            if (mutInvoker == null) {
                final List<Mut> events = new ArrayList<>(this.events);
                if (key.comparator() != null) {
                    events.sort(key.comparator());
                }
                mutInvoker = factory.invoker(events);
            }
            if (terminalInvoker == null) {
                final List<Mut> events = new ArrayList<>(terminals);
                if (key.comparator() != null) {
                    events.sort(key.comparator());
                }
                terminalInvoker = factory.delay(factory.invoker(events), runnable -> context.addCommitCallback(runnable::run));
            }
            if (dualInvoker == null) {
                dualInvoker = factory.invoker(List.of(mutInvoker, terminalInvoker));
            }
            return dualInvoker;
        }

        @Override
        protected void revertDelta(final Delta<Mut> delta) {
            if (delta instanceof final Add<Mut> add) {
                events = add.events;
                mutInvoker = null;
                dualInvoker = null;
            } else if (delta instanceof final Kill<Mut> kill) {
                events = kill.events;
                mutInvoker = null;
                dualInvoker = null;
            }
        }

        public void kill(final Object o, final boolean terminal, final BattleTransactionContext context) {
            if (terminal) {
                terminals.remove(o);
                terminalInvoker = null;
            } else {
                final ReferenceLinkedOpenHashSet<Mut> s = events;
                events = new ReferenceLinkedOpenHashSet<>(s);
                events.remove(o);
                mutInvoker = null;
                delta(context, new Kill<>(s));
            }
            dualInvoker = null;
        }

        public Token registerTerminalView(final View view) {
            final Mut converted = factory.convert(view);
            terminals.add(converted);
            terminalInvoker = null;
            dualInvoker = null;
            return new TokenImpl(converted, true, this);
        }
    }

    private static final class TokenImpl implements Token {
        private final Object o;
        private final boolean terminal;
        private final Entry<?, ?> entry;

        private TokenImpl(final Object o, final boolean terminal, final Entry<?, ?> entry) {
            this.o = o;
            this.terminal = terminal;
            this.entry = entry;
        }

        @Override
        public boolean alive() {
            if (terminal) {
                return entry.terminals.contains(o);
            }
            return entry.events.contains(o);
        }

        @Override
        public void kill(final BattleTransactionContext transactionContext) {
            if (alive()) {
                entry.kill(o, terminal, transactionContext);
            }
        }
    }

    public sealed interface Delta<T> {
    }

    private record Add<T>(ReferenceLinkedOpenHashSet<T> events) implements Delta<T> {
    }

    private record Kill<T>(ReferenceLinkedOpenHashSet<T> events) implements Delta<T> {
    }
}
