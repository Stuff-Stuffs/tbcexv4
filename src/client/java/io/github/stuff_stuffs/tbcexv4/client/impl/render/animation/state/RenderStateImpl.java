package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.PropertyType;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.*;

public class RenderStateImpl implements RenderState {
    private final Map<PropertyType<?>, Map<String, PropertyImpl<?>>> map;
    private final Map<AnimationContext, List<EventKey>> keys;
    private final SortedMap<EventKey, Event> events;
    private long nextId;
    private double lastTime;

    public RenderStateImpl() {
        map = new Reference2ObjectOpenHashMap<>();
        keys = new Object2ReferenceOpenHashMap<>();
        events = new Object2ObjectAVLTreeMap<>();
        nextId = 0;
    }

    public void update(final double time) {
        if (lastTime < time) {
            final SortedMap<EventKey, Event> headMap = events.headMap(new EventKey(null, time, Long.MIN_VALUE));
            final SortedMap<EventKey, Event> tail = headMap.tailMap(new EventKey(null, lastTime, Long.MIN_VALUE));
            for (final Event event : tail.values()) {
                event.apply();
            }
        } else if (time < lastTime) {
            final SortedMap<EventKey, Event> tailMap = events.tailMap(new EventKey(null, lastTime, Long.MIN_VALUE));
            final SortedMap<EventKey, Event> tail = tailMap.headMap(new EventKey(null, time, Long.MIN_VALUE));
            for (final Event event : tail.values()) {
                event.undo();
            }
        }
        for (final Map<String, PropertyImpl<?>> propertyMap : map.values()) {
            for (final PropertyImpl<?> value : propertyMap.values()) {
                value.compute(time);
            }
        }
        lastTime = time;
    }

    @Override
    public <T> Optional<Property<T>> getProperty(final String id, final PropertyType<T> type) {
        final Map<String, PropertyImpl<?>> propertyMap = map.get(type);
        if (propertyMap == null) {
            return Optional.empty();
        }
        final Property<?> property = propertyMap.get(id);
        //noinspection unchecked
        return Optional.of((Property<T>) property);
    }

    @Override
    public <T> Property<T> getOrCreateProperty(final String id, final PropertyType<T> type, final T defaultValue) {
        final Map<String, PropertyImpl<?>> propertyMap = map.computeIfAbsent(type, t -> new Object2ReferenceOpenHashMap<>());
        //noinspection unchecked
        PropertyImpl<T> property = (PropertyImpl<T>) propertyMap.get(id);
        if (property == null) {
            property = new PropertyImpl<>(this, id, type, defaultValue);
            propertyMap.put(id, property);
        }
        return property;
    }

    @Override
    public void addEvent(final Event event, final double time, final AnimationContext context) {
        final EventKey key = new EventKey(context, time, nextId++);
        keys.computeIfAbsent(context, i -> new ArrayList<>()).add(key);
        events.put(key, event);
        if (time < lastTime) {
            event.apply();
        }
    }

    @Override
    public void clearEvents(final AnimationContext context) {
        final List<EventKey> keyList = keys.get(context);
        for (final EventKey key : keyList) {
            events.remove(key);
        }
    }

    private record EventKey(AnimationContext context, double time, long id) implements Comparable<EventKey> {
        @Override
        public int compareTo(final EventKey o) {
            final int c = Double.compare(time, o.time);
            if (c != 0) {
                return c;
            }
            return Long.compare(id, o.id);
        }
    }
}
