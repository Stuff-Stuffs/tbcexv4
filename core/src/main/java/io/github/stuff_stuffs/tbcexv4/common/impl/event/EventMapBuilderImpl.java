package io.github.stuff_stuffs.tbcexv4.common.impl.event;

import io.github.stuff_stuffs.event_gen.api.event.EventKey;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.Map;

public class EventMapBuilderImpl implements EventMap.Builder {
    private final Reference2ReferenceMap<EventKey<?, ?>, EventKey.Factory<?, ?>> factories = new Reference2ReferenceOpenHashMap<>();

    @Override
    public <Mut, View> EventMap.Builder add(final EventKey<Mut, View> key, final EventKey.Factory<Mut, View> factory) {
        factories.put(key, factory);
        return this;
    }

    @Override
    public EventMap build() {
        final Map<EventKey<?, ?>, EventMapImpl.Entry<?, ?>> map = new Reference2ReferenceOpenHashMap<>();
        for (final Map.Entry<EventKey<?, ?>, EventKey.Factory<?, ?>> entry : factories.entrySet()) {
            map.put(entry.getKey(), create(entry.getKey(), entry.getValue()));
        }
        return new EventMapImpl(map);
    }

    private <Mut, View> EventMapImpl.Entry<Mut, View> create(final EventKey<Mut, View> key, final EventKey.Factory<?, ?> factory) {
        //noinspection unchecked
        return new EventMapImpl.Entry<>(key, (EventKey.Factory<Mut, View>) factory);
    }
}
