package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.property;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyType;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PropertyImpl<T> implements Property<T> {
    protected static final ReservationLevel[] LEVELS = ReservationLevel.values();
    protected final PropertyType<T> type;
    protected final List<DefaultValueEvent<T>> defaultValues;
    protected final Map<AnimationContext, Set<DefaultValueEvent<T>>> valuesByContext;
    protected final LevelManager<T>[] levelManagers;
    protected final boolean bridgeMode;
    protected T bridgeValue;
    protected long nextId = 0;
    protected T value;

    public PropertyImpl(final PropertyType<T> type, final T value, final boolean bridge) {
        bridgeMode = bridge;
        this.type = type;
        defaultValues = new ArrayList<>();
        valuesByContext = new Object2ReferenceOpenHashMap<>();
        this.value = value;
        if (!bridge) {
            defaultValues.add(new DefaultValueEvent<>(Double.NEGATIVE_INFINITY, value, nextId++));
        } else {
            bridgeValue = value;
        }
        levelManagers = new LevelManager[LEVELS.length];
        for (final ReservationLevel level : LEVELS) {
            levelManagers[level.ordinal()] = new LevelManager<>(this);
        }
    }

    public PropertyImpl(final PropertyType<T> type, final T defaultValue) {
        bridgeMode = false;
        this.type = type;
        defaultValues = new ArrayList<>();
        valuesByContext = new Object2ReferenceOpenHashMap<>();
        value = defaultValue;
        defaultValues.add(new DefaultValueEvent<>(Double.NEGATIVE_INFINITY, defaultValue, nextId++));
        levelManagers = new LevelManager[LEVELS.length];
        for (final ReservationLevel level : LEVELS) {
            levelManagers[level.ordinal()] = new LevelManager<>(this);
        }
    }

    public void setBridgeValue(final T value) {
        if (!isBridge()) {
            throw new RuntimeException();
        }
        bridgeValue = value;
    }

    public boolean isBridge() {
        return bridgeMode;
    }

    public void clearUpTo(final double time) {
        if (!bridgeMode) {
            final int i = searchDefaultValues(time);
            if (i > 0) {
                defaultValues.subList(0, i).clear();
            }
        }
        for (final LevelManager<T> manager : levelManagers) {
            manager.clearUpTo(time);
        }
    }

    public void checkpoint() {
        valuesByContext.clear();
    }

    @Override
    public PropertyType<T> type() {
        return type;
    }

    @Override
    public Result<Animation.TimedEvent, Unit> reserve(final Animation.StateModifier<T> modifier, final double startTime, double endTime, final Easing inOut, final AnimationContext context, final ReservationLevel level) {
        if (context.cutoff() <= startTime) {
            return new Result.Success<>(new TimedEventImpl<>(modifier, startTime, startTime, inOut, -1, context));
        } else if (context.cutoff() < endTime) {
            endTime = context.cutoff();
        }
        return levelManagers[level.ordinal()].reserve(modifier, startTime, endTime, inOut, context);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> setDefaultValue(final T val, final double time, final AnimationContext context) {
        if (bridgeMode) {
            return Result.failure(Unit.INSTANCE);
        }
        final DefaultValueEvent<T> event = new DefaultValueEvent<>(time, val, nextId++);
        final int index = Collections.binarySearch(defaultValues, event);
        if (index >= 0) {
            throw new IllegalStateException();
        }
        final int rIndex = -index - 1;
        if (rIndex != defaultValues.size()) {
            return Result.failure(Unit.INSTANCE);
        }
        defaultValues.add(event);
        valuesByContext.computeIfAbsent(context, k -> new ObjectOpenHashSet<>()).add(event);
        return Result.success(new Animation.TimedEvent() {
            @Override
            public double start() {
                return time;
            }

            @Override
            public double end() {
                return time;
            }
        });
    }

    public void clearAll(final AnimationContext context) {
        final Set<DefaultValueEvent<T>> removed = valuesByContext.remove(context);
        if (removed != null) {
            defaultValues.removeAll(removed);
        }
        for (final ReservationLevel level : LEVELS) {
            levelManagers[level.ordinal()].clearAll(context);
        }
    }

    private int searchDefaultValues(final double time) {
        final int size = defaultValues.size();
        final int index;
        outer:
        {
            int low = 0;
            int high = defaultValues.size() - 1;

            while (low <= high) {
                final int mid = (low + high) >>> 1;
                final DefaultValueEvent<T> midVal = defaultValues.get(mid);
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
        final int rIndex;
        if (index >= 0) {
            rIndex = index;
        } else {
            rIndex = Math.max(Math.min(size - 1, -index - 2), 0);
        }
        return rIndex;
    }

    public void compute(final double time) {
        T val = bridgeMode ? bridgeValue : defaultValues.get(searchDefaultValues(time)).val;
        for (final LevelManager<T> manager : levelManagers) {
            val = manager.compute(val, time);
        }
        value = val;
    }

    @Override
    public T get() {
        return value;
    }

    public double lastAbove(@Nullable final ReservationLevel level) {
        double last = Double.NEGATIVE_INFINITY;
        for (final DefaultValueEvent<T> defaultValue : defaultValues) {
            last = Math.max(last, defaultValue.time);
        }
        for (final ReservationLevel reservationLevel : LEVELS) {
            if (level == null || level.ordinal() < reservationLevel.ordinal()) {
                last = Math.max(last, levelManagers[reservationLevel.ordinal()].lastEvent());
            }
        }
        return last;
    }

    protected static final class LevelManager<T> {
        private final PropertyImpl<T> parent;
        private final List<TimedEventImpl<T>> modifiers;
        private final Map<AnimationContext, List<TimedEventImpl<T>>> map;
        private long nextId;

        private LevelManager(final PropertyImpl<T> parent) {
            this.parent = parent;
            modifiers = new ArrayList<>();
            map = new Object2ReferenceOpenHashMap<>();
        }

        public void clearUpTo(final double time) {
            for (final Iterator<TimedEventImpl<T>> iterator = modifiers.iterator(); iterator.hasNext(); ) {
                final TimedEventImpl<T> modifier = iterator.next();
                if (modifier.end < time) {
                    iterator.remove();
                    final List<TimedEventImpl<T>> events = map.get(modifier.context);
                    if (events != null) {
                        if (events.remove(modifier)) {
                            if (events.isEmpty()) {
                                map.remove(modifier.context);
                            }
                        }
                    }
                }
            }
        }

        private int advance(int index, final double time) {
            while (index < modifiers.size() && modifiers.get(index).end < time) {
                index++;
            }
            return index;
        }

        Result<Animation.TimedEvent, Unit> reserve(final Animation.StateModifier<T> stateModifier, final double startTime, final double endTime, final Easing inout, final AnimationContext id) {
            final TimedEventImpl<T> event = new TimedEventImpl<>(stateModifier, startTime, endTime, inout, nextId++, id);
            final int index = Collections.binarySearch(modifiers, event);
            if (index >= 0) {
                final int rIndex = advance(index, startTime);
                if (rIndex == modifiers.size()) {
                    modifiers.add(rIndex, event);
                    map.computeIfAbsent(id, i -> new ArrayList<>()).add(event);
                    return new Result.Success<>(event);
                }
                return new Result.Failure<>(Unit.INSTANCE);
            }
            final int rIndex = -index - 1;
            if (rIndex < modifiers.size()) {
                final TimedEventImpl<T> next = modifiers.get(rIndex);
                if (endTime > next.start) {
                    return new Result.Failure<>(Unit.INSTANCE);
                }
            }
            if (rIndex > 0) {
                final TimedEventImpl<T> prev = modifiers.get(rIndex - 1);
                if (prev.end > startTime) {
                    return new Result.Failure<>(Unit.INSTANCE);
                }
            }
            modifiers.add(rIndex, event);
            map.computeIfAbsent(id, i -> new ArrayList<>()).add(event);
            return new Result.Success<>(event);
        }

        T compute(final T lower, final double time) {
            if (modifiers.isEmpty()) {
                return lower;
            }
            final int index;
            outer:
            {
                int low = 0;
                int high = modifiers.size() - 1;

                while (low <= high) {
                    final int mid = (low + high) >>> 1;
                    final TimedEventImpl<T> midVal = modifiers.get(mid);
                    final int cmp = Double.compare(midVal.start, time);

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
            final TimedEventImpl<T> modifier;
            if (index >= 0) {
                modifier = modifiers.get(index);
            } else {
                final int rIndex = -index - 1;
                if (rIndex == 0) {
                    return lower;
                } else {
                    final TimedEventImpl<T> prev = modifiers.get(rIndex - 1);
                    if (prev.end < time) {
                        return lower;
                    }
                    modifier = prev;
                }
            }
            return parent.type.interpolator().interpolate(lower, modifier.modifier.value(time), modifier.inout.ease(time));
        }

        public void clearAll(final AnimationContext id) {
            final List<TimedEventImpl<T>> list = map.remove(id);
            if (list != null) {
                for (final TimedEventImpl<T> modifier : list) {
                    modifiers.remove(modifier);
                }
            }
        }

        public double lastEvent() {
            if (modifiers.isEmpty()) {
                return Double.NEGATIVE_INFINITY;
            }
            return modifiers.get(modifiers.size() - 1).end;
        }
    }

    private static final class TimedEventImpl<T> implements Animation.TimedEvent, Comparable<TimedEventImpl<?>> {
        private final Animation.StateModifier<T> modifier;
        private final double start;
        private final double end;
        private final Easing inout;
        private final long id;
        private final AnimationContext context;

        public TimedEventImpl(final Animation.StateModifier<T> modifier, final double start, final double end, final Easing inout, final long id, final AnimationContext context) {
            this.modifier = modifier;
            this.start = start;
            this.end = end;
            this.inout = inout;
            this.id = id;
            this.context = context;
        }

        @Override
        public double start() {
            return start;
        }

        @Override
        public double end() {
            return end;
        }

        @Override
        public int compareTo(final TimedEventImpl<?> o) {
            final int c = Double.compare(start, o.start);
            if (c != 0) {
                return c;
            }
            return Long.compare(id, o.id);
        }
    }

    protected record DefaultValueEvent<T>(double time, T val, long id) implements Comparable<DefaultValueEvent<?>> {
        @Override
        public int compareTo(final DefaultValueEvent<?> o) {
            final int c = Double.compare(time, o.time);
            if (c != 0) {
                return c;
            }
            return Long.compare(id, o.id);
        }
    }
}
