package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TimedContainer<K, V extends RenderStateImpl> {
    public static final int INVALID_SERIAL_NUMBER = -1;
    private final Object2ReferenceMap<K, Container> containers;
    private final Map<Key<K>, V> values;
    private final Function<K, V> factory;

    public TimedContainer(final Function<K, V> factory) {
        this.factory = factory;
        containers = new Object2ReferenceOpenHashMap<>();
        values = new Object2ReferenceOpenHashMap<>();
    }

    public @Nullable V get(final K k, final double time) {
        final int serialNumber = serialNumber(k, time);
        if (serialNumber == INVALID_SERIAL_NUMBER) {
            return null;
        }
        return values.computeIfAbsent(new Key<>(k, serialNumber), i -> factory.apply(k));
    }

    public Set<K> children(final double time) {
        final Set<K> result = new ObjectOpenHashSet<>();
        for (final Object2ReferenceMap.Entry<K, Container> entry : Object2ReferenceMaps.fastIterable(containers)) {
            if (entry.getValue().serialNumber(time) != INVALID_SERIAL_NUMBER) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void update(final double time) {
        for (final Object2ReferenceMap.Entry<K, Container> entry : Object2ReferenceMaps.fastIterable(containers)) {
            final int serialNumber = entry.getValue().serialNumber(time);
            if (serialNumber != INVALID_SERIAL_NUMBER) {
                final V value = values.get(new Key<>(entry.getKey(), serialNumber));
                if (value != null) {
                    value.update(time);
                }
            }
        }
    }

    public int serialNumber(final K key, final double time) {
        final Container container = containers.get(key);
        if (key == null) {
            return INVALID_SERIAL_NUMBER;
        }
        return container.serialNumber(time);
    }

    public Result<Animation.TimedEvent, Unit> add(final K key, final double time, final AnimationContext context) {
        if (context.cutoff() < time) {
            return Result.success(new TimedEventImpl(context.cutoff()));
        }
        return containers.computeIfAbsent(key, k -> new Container()).addOrRemove(time, context, true);
    }

    public Result<Animation.TimedEvent, Unit> remove(final K key, final double time, final AnimationContext context) {
        if (context.cutoff() < time) {
            return Result.success(new TimedEventImpl(context.cutoff()));
        }
        return containers.computeIfAbsent(key, k -> new Container()).addOrRemove(time, context, false);
    }

    public void clear(final AnimationContext context, final double time) {
        for (final Map.Entry<K, Container> entry : containers.entrySet()) {
            final K key = entry.getKey();
            final int serial = serialNumber(key, time);
            if (serial != INVALID_SERIAL_NUMBER) {
                values.get(new Key<>(key, serial)).cleanup(context, time);
            }
            final Container container = entry.getValue();
            container.clear(context);
        }
    }

    private record Entry(double time, boolean add, long id) implements Comparable<Entry> {
        @Override
        public int compareTo(final Entry o) {
            final int c = Double.compare(time, o.time);
            if (c != 0) {
                return c;
            }
            return Long.compare(id, o.id);
        }
    }

    private static final class Container {
        private final List<Entry> entries;
        private final Map<AnimationContext, Set<Entry>> map;
        private long nextId;

        private Container() {
            entries = new ArrayList<>();
            map = new Object2ReferenceOpenHashMap<>();
        }

        private int serialNumber(final double time) {
            final Entry k = new Entry(time, true, Long.MAX_VALUE);
            final int index = Collections.binarySearch(entries, k);
            if (index >= 0) {
                final int advancedIndex = advance(index, time);
                final Entry entry = entries.get(advancedIndex);
                return entry.add ? advancedIndex / 2 : INVALID_SERIAL_NUMBER;
            }
            final int rIndex = -index - 1;
            if (rIndex == 0) {
                return 0;
            }
            final int advancedIndex = advance(rIndex - 1, time);
            final Entry entry = entries.get(advancedIndex);
            return entry.add ? advancedIndex / 2 : INVALID_SERIAL_NUMBER;
        }

        private int advance(int index, final double time) {
            while (index + 1 < entries.size() && entries.get(index + 1).time == time) {
                index++;
            }
            return index;
        }

        private Result<Animation.TimedEvent, Unit> addOrRemove(final double time, final AnimationContext context, final boolean add) {
            final Entry key = new Entry(time, add, nextId++);
            int index = Collections.binarySearch(entries, key);
            if (index >= 0) {
                index = advance(index, time);
                if (index + 1 != entries.size()) {
                    return Result.failure(Unit.INSTANCE);
                }
                final Entry entry = entries.get(index);
                if (entry.add == add) {
                    return Result.failure(Unit.INSTANCE);
                }
                entries.add(key);
                map.computeIfAbsent(context, i -> new ObjectOpenHashSet<>()).add(key);
                return Result.success(new TimedEventImpl(time));
            }
            if (entries.isEmpty()) {
                if (add) {
                    entries.add(key);
                    map.computeIfAbsent(context, i -> new ObjectOpenHashSet<>()).add(key);
                    return Result.success(new TimedEventImpl(time));
                } else {
                    return Result.failure(Unit.INSTANCE);
                }
            }
            final int rIndex = advance(-index - 1, time);
            if (rIndex != entries.size()) {
                return Result.failure(Unit.INSTANCE);
            }
            final Entry entry = entries.get(entries.size() - 1);
            if (entry.add == add) {
                return Result.failure(Unit.INSTANCE);
            }
            entries.add(key);
            map.computeIfAbsent(context, i -> new ObjectOpenHashSet<>()).add(key);
            return Result.success(new TimedEventImpl(time));
        }

        public void clear(final AnimationContext context) {
            final Set<Entry> toRemove = map.remove(context);
            if (toRemove != null && !toRemove.isEmpty()) {
                entries.removeAll(toRemove);
            }
        }
    }

    private static final class TimedEventImpl implements Animation.TimedEvent {
        private final double time;

        private TimedEventImpl(final double time) {
            this.time = time;
        }

        @Override
        public double start() {
            return time;
        }

        @Override
        public double end() {
            return time;
        }
    }

    private record Key<K>(K key, int serialNumber) {
    }
}
