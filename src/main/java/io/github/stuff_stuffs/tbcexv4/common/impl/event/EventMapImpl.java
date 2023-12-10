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
    public <Mut> Mut invoker(final EventKey<Mut, ?> key) {
        if (!contains(key)) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return ((Entry<Mut, ?>) eventEntries.get(key)).invoker();
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
    public boolean contains(final EventKey<?, ?> key) {
        return eventEntries.containsKey(key);
    }

    static final class Entry<Mut, View> extends DeltaSnapshotParticipant<Delta<Mut>> {
        private final EventKey<Mut, View> key;
        private final EventKey.Factory<Mut, View> factory;
        private ReferenceLinkedOpenHashSet<Mut> events;
        private Mut invoker;

        Entry(final EventKey<Mut, View> key, final EventKey.Factory<Mut, View> factory) {
            this.key = key;
            this.factory = factory;
            events = new ReferenceLinkedOpenHashSet<>();
        }


        public Token registerView(final View view, final BattleTransactionContext transactionContext) {
            return register(factory.convert(view), transactionContext);
        }

        public Token register(final Mut mut, final BattleTransactionContext transactionContext) {
            final ReferenceLinkedOpenHashSet<Mut> s = events;
            events = new ReferenceLinkedOpenHashSet<>(s);
            events.addAndMoveToLast(mut);
            invoker = null;
            delta(transactionContext, new Add<>(s));
            return new TokenImpl(mut, this);
        }

        public Mut invoker() {
            if (invoker == null) {
                final List<Mut> events = new ArrayList<>(this.events);
                if (key.comparator() != null) {
                    events.sort(key.comparator());
                }
                invoker = factory.invoker(events);
            }
            return invoker;
        }

        @Override
        protected void revertDelta(final Delta<Mut> delta) {
            if (delta instanceof Add<Mut> add) {
                events = add.events;
                invoker = null;
            } else if (delta instanceof Kill<Mut> kill) {
                events = kill.events;
                invoker = null;
            }
        }

        public void kill(final Object o, final BattleTransactionContext context) {
            final ReferenceLinkedOpenHashSet<Mut> s = events;
            events = new ReferenceLinkedOpenHashSet<>(s);
            events.remove(o);
            invoker = null;
            delta(context, new Kill<>(s));
        }
    }

    private static final class TokenImpl implements Token {
        private final Object o;
        private final Entry<?, ?> entry;

        private TokenImpl(final Object o, final Entry<?, ?> entry) {
            this.o = o;
            this.entry = entry;
        }

        @Override
        public boolean alive() {
            return entry.events.contains(o);
        }

        @Override
        public void kill(final BattleTransactionContext transactionContext) {
            if (alive()) {
                entry.kill(o, transactionContext);
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
