package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.PropertyType;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.*;

public class PropertyImpl<T> implements Property<T> {
    private static final ReservationLevel[] LEVELS = ReservationLevel.values();
    private final RenderState parent;
    private final String id;
    private final PropertyType<T> type;
    private T defaultValue;
    private final Map<ReservationLevel, LevelManager<T>> levelManagers;
    private T value;

    public PropertyImpl(final RenderState parent, final String id, final PropertyType<T> type, final T defaultValue) {
        this.parent = parent;
        this.id = id;
        this.type = type;
        this.defaultValue = defaultValue;
        value = defaultValue;
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
    public void setDefaultValue(final T val) {
        defaultValue = val;
    }

    @Override
    public Result<Animation.AppliedStateModifier<T>, Unit> reserve(final Animation.StateModifier<T> modifier, final double startTime, double endTime, final Easing inOut, final AnimationContext context, final ReservationLevel level) {
        if (context.cutoff() <= startTime) {
            return new Result.Success<>(new AppliedStateModifierImpl<>(modifier, id, type, parent, new Interval(startTime, startTime, inOut)));
        } else if (context.cutoff() < endTime) {
            endTime = context.cutoff();
        }
        return levelManagers.get(level).reserve(modifier, startTime, endTime, inOut, context);
    }

    @Override
    public void clearAll(final AnimationContext id) {
        for (final ReservationLevel level : LEVELS) {
            levelManagers.get(level).clearAll(id);
        }
    }

    public void compute(final double time) {
        T val = defaultValue;
        for (final ReservationLevel level : LEVELS) {
            val = levelManagers.get(level).compute(val, time);
        }
        value = val;
    }

    @Override
    public T get() {
        return value;
    }

    private static final class LevelManager<T> {
        private final PropertyImpl<T> parent;
        private final List<Interval> intervals;
        private final List<AppliedStateModifierImpl<T>> modifiers;
        private final Map<AnimationContext, List<AppliedStateModifierImpl<T>>> map;

        private LevelManager(final PropertyImpl<T> parent) {
            this.parent = parent;
            intervals = new ArrayList<>();
            modifiers = new ArrayList<>();
            map = new Object2ReferenceOpenHashMap<>();
        }

        private Result<Animation.AppliedStateModifier<T>, Unit> reserve(final Animation.StateModifier<T> modifier, final double startTime, final double endTime, final Easing inout, final AnimationContext id) {
            final Interval interval = new Interval(startTime, endTime, inout);
            final int index = Collections.binarySearch(intervals, interval);
            if (index >= 0) {
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
            final AppliedStateModifierImpl<T> applied = new AppliedStateModifierImpl<>(modifier, parent.id, parent.type, parent.parent, interval);
            modifiers.add(rIndex, applied);
            map.computeIfAbsent(id, i -> new ArrayList<>()).add(applied);
            return new Result.Success<>(applied);
        }

        private T compute(final T lower, final double time) {
            final Interval interval = new Interval(time, 0, null);
            final int index = Collections.binarySearch(intervals, interval);
            final AppliedStateModifierImpl<T> modifier;
            if (index >= 0) {
                modifier = modifiers.get(index);
            } else {
                final int rIndex = -index - 1;
                if (rIndex == 0) {
                    return lower;
                } else {
                    final AppliedStateModifierImpl<T> prev = modifiers.get(rIndex - 1);
                    if (prev.interval.end < time) {
                        return lower;
                    }
                    modifier = prev;
                }
            }
            return parent.type.interpolator().interpolate(lower, modifier.modifier.value(time), modifier.interval.inout.ease(time));
        }

        public void clearAll(final AnimationContext id) {
            final List<AppliedStateModifierImpl<T>> list = map.remove(id);
            if (list != null) {
                for (final AppliedStateModifierImpl<T> modifier : list) {
                    modifiers.remove(modifier);
                    intervals.remove(modifier.interval);
                }
            }
        }
    }

    public record Interval(double start, double end, Easing inout) implements Comparable<Interval> {
        @Override
        public int compareTo(final Interval o) {
            return Double.compare(start, o.start);
        }
    }

    public static final class AppliedStateModifierImpl<T> implements Animation.AppliedStateModifier<T> {
        private final Animation.StateModifier<T> modifier;
        private final String id;
        private final PropertyType<T> type;
        private final RenderState state;
        private final Interval interval;

        public AppliedStateModifierImpl(final Animation.StateModifier<T> modifier, final String id, final PropertyType<T> type, final RenderState state, final Interval interval) {
            this.modifier = modifier;
            this.id = id;
            this.type = type;
            this.state = state;
            this.interval = interval;
        }

        @Override
        public double start() {
            return interval.start;
        }

        @Override
        public double end() {
            return interval.end;
        }

        @Override
        public Easing inout() {
            return interval.inout;
        }

        @Override
        public Animation.StateModifier<T> modifier() {
            return modifier;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public PropertyType<T> type() {
            return type;
        }

        @Override
        public RenderState state() {
            return state;
        }
    }
}
