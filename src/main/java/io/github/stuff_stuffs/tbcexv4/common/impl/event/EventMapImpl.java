package io.github.stuff_stuffs.tbcexv4.common.impl.event;

import io.github.stuff_stuffs.event_gen.api.event.EventKey;
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
    public <Mut> Token registerMut(final EventKey<Mut, ?> key, final Mut event) {
        if (!contains(key)) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return ((Entry<Mut, ?>) eventEntries.get(key)).register(event);
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
    public <View> Token registerView(final EventKey<?, View> key, final View view) {
        if (!contains(key)) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return ((Entry<?, View>) eventEntries.get(key)).registerView(view);
    }

    @Override
    public boolean contains(final EventKey<?, ?> key) {
        return eventEntries.containsKey(key);
    }

    static final class Entry<Mut, View> {
        private final EventKey<Mut, View> key;
        private final EventKey.Factory<Mut, View> factory;
        private final ReferenceLinkedOpenHashSet<Mut> events;
        private Mut invoker;

        Entry(final EventKey<Mut, View> key, final EventKey.Factory<Mut, View> factory) {
            this.key = key;
            this.factory = factory;
            events = new ReferenceLinkedOpenHashSet<>();
        }


        public Token registerView(final View view) {
            return register(factory.convert(view));
        }

        public Token register(final Mut mut) {
            events.add(mut);
            invoker = null;
            return new TokenImpl(mut, this);
        }

        public void remove(final Object o) {
            if (events.remove(o)) {
                invoker = null;
            }
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
        public void kill() {
            if (alive()) {
                entry.remove(o);
            }
        }
    }
}
