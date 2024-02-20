package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class TimedContainer<K, V extends RenderStateImpl> {
    public static final int INVALID_SERIAL_NUMBER = -1;
    private final Object2ReferenceMap<K, Container<V>> containers;
    private final Function<K, V> factory;

    public TimedContainer(final Function<K, V> factory) {
        this.factory = factory;
        containers = new Object2ReferenceLinkedOpenHashMap<>();
    }

    public V get(final K k, final double time) {
        final Container<V> container = containers.get(k);
        if (container == null) {
            return null;
        }
        final int serialNumber = container.serialNumber(time);
        if (serialNumber == INVALID_SERIAL_NUMBER) {
            return null;
        }
        final Int2ObjectMap<V> values = container.values;
        V value = values.get(serialNumber);
        if (value != null) {
            return value;
        }
        value = factory.apply(k);
        values.put(serialNumber, value);
        return value;
    }

    public Set<K> children(final double time) {
        final Set<K> result = new ObjectOpenHashSet<>();
        for (final Object2ReferenceMap.Entry<K, Container<V>> entry : Object2ReferenceMaps.fastIterable(containers)) {
            if (entry.getValue().serialNumber(time) != INVALID_SERIAL_NUMBER) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public int update(final double time) {
        int flags = 0;
        for (final Object2ReferenceMap.Entry<K, Container<V>> entry : Object2ReferenceMaps.fastIterable(containers)) {
            final Container<V> container = entry.getValue();
            final int serialNumber = container.serialNumber(time);
            if (serialNumber != INVALID_SERIAL_NUMBER) {
                final V value = container.values.get(serialNumber);
                if (value != null) {
                    flags |= value.update(time);
                }
            }
        }
        return flags;
    }

    public void forceAdd(final K key, final double time) {
        containers.computeIfAbsent(key, k -> new Container<>()).forceAdd(time);
    }

    public void forceRemove(final K key, final double time, final Property.ReservationLevel level) {
        containers.computeIfAbsent(key, l -> new Container<>()).forceRemove(time, level);
    }

    public Result<Animation.TimedEvent, Unit> add(final K key, final double time, final AnimationContext context) {
        if (context.cutoff() < time) {
            return Result.success(new TimedEventImpl(context.cutoff()));
        }
        return containers.computeIfAbsent(key, k -> new Container<>()).addOrRemove(time, context, true);
    }

    public Result<Animation.TimedEvent, Unit> remove(final K key, final double time, final AnimationContext context) {
        if (context.cutoff() < time) {
            return Result.success(new TimedEventImpl(context.cutoff()));
        }
        return containers.computeIfAbsent(key, k -> new Container<>()).addOrRemove(time, context, false);
    }

    public void clear(final AnimationContext context, final double time) {
        for (final Container<V> container : containers.values()) {
            container.clear(context);
            final int serial = container.serialNumber(time);
            if (serial != INVALID_SERIAL_NUMBER) {
                container.values.get(serial).cleanup(context, time);
            }
        }
    }

    public void checkpoint() {
        for (final Container<V> container : containers.values()) {
            for (final V value : container.values.values()) {
                value.checkpoint();
            }
        }
    }

    public void clearUpTo(final double time) {
        containers.values().removeIf(value -> value.clearUpTo(time));
    }

    private record Entry(double time, boolean add, long id,
                         @Nullable AnimationContext context) implements Comparable<Entry> {
        @Override
        public int compareTo(final Entry o) {
            final int c = Double.compare(time, o.time);
            if (c != 0) {
                return c;
            }
            return Long.compare(id, o.id);
        }
    }

    private static final class Container<V extends RenderStateImpl> {
        private final List<Entry> entries;
        private final Map<AnimationContext, Set<Entry>> map;
        private final Int2ObjectMap<V> values;
        private long nextId;

        private Container() {
            entries = new ArrayList<>();
            map = new Object2ReferenceOpenHashMap<>();
            values = new Int2ObjectOpenHashMap<>();
        }

        private int serialNumber(final double time) {
            final int index;
            outer:
            {
                int low = 0;
                int high = entries.size() - 1;

                while (low <= high) {
                    final int mid = (low + high) >>> 1;
                    final Entry midVal = entries.get(mid);
                    final int cmp = Double.compare(midVal.time, time);

                    if (cmp < 0) {
                        low = mid + 1;
                    } else if (cmp > 0) {
                        high = mid - 1;
                    } else {
                        index = mid;
                        break outer;
                    }
                }
                index = -(low + 1);
            }
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
            final Entry key = new Entry(time, add, nextId++, context);
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

        public boolean clearUpTo(final double time) {
            int clearIndex = -1;
            final List<Entry> list = entries;
            final int size = list.size();
            for (int i = 1; i < size; i += 2) {
                if (list.get(i).time < time) {
                    clearIndex = i;
                }
            }
            for (int i = 0; i < clearIndex; i++) {
                final Entry entry = list.get(i);
                if (entry.context == null) {
                    continue;
                }
                final Set<Entry> set = map.get(entry.context);
                if (set != null) {
                    if (set.remove(entry)) {
                        if (set.isEmpty()) {
                            map.remove(entry.context);
                        }
                    }
                }
            }
            clearRange(0, clearIndex + 1);
            return list.isEmpty();
        }

        private void clearRange(final int start, final int end) {
            final List<Entry> subList = entries.subList(start, end);
            final List<Entry> copy = new ArrayList<>(subList);
            subList.clear();
            for (final Entry entry : copy) {
                values.remove(serialNumber(entry.time));
            }
        }

        public void forceAdd(final double time) {
            final Entry key = new Entry(time, true, nextId++, null);
            if (entries.isEmpty()) {
                entries.add(key);
            }
            final int index = Collections.binarySearch(entries, key);
            if (index >= 0) {
                if (index == 0) {
                    clearRange(0, entries.size());
                    entries.add(new Entry(time, true, nextId++, null));
                }
                final Entry prev = entries.get(index - 1);
                if (prev.add) {
                    entries.add(index, new Entry(time, false, nextId++, null));
                    entries.add(index + 1, key);
                    final int size = entries.size();
                    if (index + 2 < size) {
                        clearRange(index + 2, size);
                    }
                } else {
                    entries.add(index, key);
                    final int size = entries.size();
                    if (index + 1 < size) {
                        clearRange(index + 1, size);
                    }
                }
                return;
            }
            final int rIndex = advance(-index - 1, time);
            if (rIndex == 0) {
                clearRange(0, entries.size());
                entries.add(key);
                return;
            }
            final Entry prev = entries.get(rIndex - 1);
            if (prev.add) {
                entries.add(rIndex, new Entry(time, false, nextId++, null));
                entries.add(rIndex + 1, key);
                final int size = entries.size();
                if (rIndex + 2 < size) {
                    clearRange(rIndex + 2, size);
                }
            } else {
                entries.add(rIndex, key);
                final int size = entries.size();
                if (rIndex + 1 < size) {
                    clearRange(rIndex + 1, size);
                }
            }
        }

        private void forceRemove0(final double time) {
            final Entry key = new Entry(time, false, nextId++, null);
            if (entries.isEmpty()) {
                return;
            }
            final int index = Collections.binarySearch(entries, key);
            if (index >= 0) {
                if (index == 0) {
                    clearRange(0, entries.size());
                    return;
                }
                final Entry prev = entries.get(index - 1);
                if (prev.add) {
                    entries.add(index, key);
                    final int size = entries.size();
                    if (index + 1 < size) {
                        clearRange(index + 1, size);
                    }
                }
                return;
            }
            final int rIndex = advance(-index - 1, time);
            if (rIndex == 0) {
                clearRange(0, entries.size());
                return;
            }
            final Entry prev = entries.get(rIndex - 1);
            if (prev.add) {
                entries.add(index, key);
                final int size = entries.size();
                if (rIndex + 1 < size) {
                    clearRange(index + 1, size);
                }
            }
        }

        public void forceRemove(double time, final Property.@Nullable ReservationLevel level) {
            final int serialNumber = serialNumber(time);
            if (serialNumber != INVALID_SERIAL_NUMBER) {
                final V v = values.get(serialNumber);
                if (v != null) {
                    final double lastAbove = v.lastAbove(time, level);
                    time = Math.max(lastAbove, time);
                }
                forceRemove0(time);
            }
        }
    }

    private record TimedEventImpl(double time) implements Animation.TimedEvent {
        @Override
        public double start() {
            return time;
        }

        @Override
        public double end() {
            return time;
        }
    }
}
