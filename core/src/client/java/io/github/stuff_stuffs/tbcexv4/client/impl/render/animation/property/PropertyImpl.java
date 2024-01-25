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
    private static final ReservationLevel[] LEVELS = ReservationLevel.values();
    private final PropertyType<T> type;
    private final List<DefaultValueEvent<T>> defaultValues;
    private final Map<AnimationContext, Set<DefaultValueEvent<T>>> valuesByContext;
    private final Map<ReservationLevel, LevelManager<T>> levelManagers;
    private long nextId = 0;
    private T value;

    public PropertyImpl(final PropertyType<T> type, final T defaultValue) {
        this.type = type;
        defaultValues = new ArrayList<>();
        valuesByContext = new Object2ReferenceOpenHashMap<>();
        value = defaultValue;
        defaultValues.add(new DefaultValueEvent<>(Double.NEGATIVE_INFINITY, defaultValue, nextId++));
        levelManagers = new EnumMap<>(ReservationLevel.class);
        for (final ReservationLevel level : LEVELS) {
            levelManagers.put(level, new LevelManager<>(this));
        }
    }

    @Override
    public PropertyType<T> type() {
        return type;
    }

    @Override
    public Result<Animation.TimedEvent, Unit> reserve(final Animation.StateModifier<T> modifier, final double startTime, double endTime, final Easing inOut, final AnimationContext context, final ReservationLevel level) {
        if (context.cutoff() <= startTime) {
            return new Result.Success<>(new TimedEventImpl<>(new Interval(startTime, startTime, inOut, 0), modifier));
        } else if (context.cutoff() < endTime) {
            endTime = context.cutoff();
        }
        return levelManagers.get(level).reserve(modifier, startTime, endTime, inOut, context);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> setDefaultValue(final T val, final double time, final AnimationContext context) {
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
            levelManagers.get(level).clearAll(context);
        }
    }

    public void compute(final double time) {
        final DefaultValueEvent<T> event = new DefaultValueEvent<>(time, null, Long.MAX_VALUE);
        final int index = Collections.binarySearch(defaultValues, event);
        final int rIndex;
        if (index >= 0) {
            rIndex = index;
        } else {
            rIndex = Math.min(defaultValues.size() - 1, -index - 2);
        }
        T val = defaultValues.get(rIndex).val;
        for (final ReservationLevel level : LEVELS) {
            val = levelManagers.get(level).compute(val, time);
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
                last = Math.max(last, levelManagers.get(reservationLevel).lastEvent());
            }
        }
        return last;
    }

    private static final class LevelManager<T> {
        private final PropertyImpl<T> parent;
        private final List<Interval> intervals;
        private final List<TimedEventImpl<T>> modifiers;
        private final Map<AnimationContext, List<TimedEventImpl<T>>> map;
        private long nextId;

        private LevelManager(final PropertyImpl<T> parent) {
            this.parent = parent;
            intervals = new ArrayList<>();
            modifiers = new ArrayList<>();
            map = new Object2ReferenceOpenHashMap<>();
        }

        private int advance(int index, final double time) {
            while (index < intervals.size() && intervals.get(index).end < time) {
                index++;
            }
            return index;
        }

        private Result<Animation.TimedEvent, Unit> reserve(final Animation.StateModifier<T> stateModifier, final double startTime, final double endTime, final Easing inout, final AnimationContext id) {
            final Interval interval = new Interval(startTime, endTime, inout, nextId++);
            final int index = Collections.binarySearch(intervals, interval);
            if (index >= 0) {
                final int rIndex = advance(index, startTime);
                if (rIndex == intervals.size()) {
                    intervals.add(rIndex, interval);
                    final TimedEventImpl<T> applied = new TimedEventImpl<>(interval, stateModifier);
                    modifiers.add(rIndex, applied);
                    map.computeIfAbsent(id, i -> new ArrayList<>()).add(applied);
                    return new Result.Success<>(applied);
                }
                return new Result.Failure<>(Unit.INSTANCE);
            }
            final int rIndex = -index - 1;
            if (rIndex < intervals.size()) {
                final Interval next = intervals.get(rIndex);
                if (endTime > next.start) {
                    return new Result.Failure<>(Unit.INSTANCE);
                }
            }
            if (rIndex > 0) {
                final Interval prev = intervals.get(rIndex - 1);
                if (prev.end > startTime) {
                    return new Result.Failure<>(Unit.INSTANCE);
                }
            }
            intervals.add(rIndex, interval);
            final TimedEventImpl<T> applied = new TimedEventImpl<>(interval, stateModifier);
            modifiers.add(rIndex, applied);
            map.computeIfAbsent(id, i -> new ArrayList<>()).add(applied);
            return new Result.Success<>(applied);
        }

        private T compute(final T lower, final double time) {
            final Interval interval = new Interval(time, 0, null, Long.MAX_VALUE);
            final int index = Collections.binarySearch(intervals, interval);
            final TimedEventImpl<T> modifier;
            if (index >= 0) {
                modifier = modifiers.get(index);
            } else {
                final int rIndex = -index - 1;
                if (rIndex == 0) {
                    return lower;
                } else {
                    final TimedEventImpl<T> prev = modifiers.get(rIndex - 1);
                    if (prev.interval.end < time) {
                        return lower;
                    }
                    modifier = prev;
                }
            }
            return parent.type.interpolator().interpolate(lower, modifier.modifier.value(time), modifier.interval.inout.ease(time));
        }

        public void clearAll(final AnimationContext id) {
            final List<TimedEventImpl<T>> list = map.remove(id);
            if (list != null) {
                for (final TimedEventImpl<T> modifier : list) {
                    modifiers.remove(modifier);
                    intervals.remove(modifier.interval);
                }
            }
        }

        public double lastEvent() {
            if (modifiers.isEmpty()) {
                return Double.NEGATIVE_INFINITY;
            }
            return modifiers.get(modifiers.size() - 1).interval.end;
        }
    }

    private record Interval(double start, double end, Easing inout, long id) implements Comparable<Interval> {
        @Override
        public int compareTo(final Interval o) {
            final int c = Double.compare(start, o.start);
            if (c != 0) {
                return c;
            }
            return Long.compare(id, o.id);
        }
    }

    private static final class TimedEventImpl<T> implements Animation.TimedEvent {
        private final Interval interval;
        private final Animation.StateModifier<T> modifier;

        public TimedEventImpl(final Interval interval, final Animation.StateModifier<T> modifier) {
            this.interval = interval;
            this.modifier = modifier;
        }

        @Override
        public double start() {
            return interval.start;
        }

        @Override
        public double end() {
            return interval.end;
        }
    }

    private record DefaultValueEvent<T>(double time, T val, long id) implements Comparable<DefaultValueEvent<?>> {
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
