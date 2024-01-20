package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.load.keyframe;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.EasingFunction;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.*;

public class ModelKeyframeAnimation implements Animation<ModelRenderState> {
    private final Map<Path, ModelEntry> entries;

    private ModelKeyframeAnimation(final Map<Path, ModelEntry> entries) {
        this.entries = entries;
    }

    @Override
    public Result<List<TimedEvent>, Unit> animate(final double time, final ModelRenderState state, final AnimationContext context) {
        final var folder = Result.<List<TimedEvent>, Unit>success(new ArrayList<>()).<List<TimedEvent>>folder((e0, e1) -> {
            e0.addAll(e1);
            return e0;
        }, (u0, u1) -> Unit.INSTANCE);
        for (final Map.Entry<Path, ModelEntry> entry : entries.entrySet()) {
            folder.accept(tryApply(entry.getKey(), 0, entry.getValue().keyframes(), time, state, context));
        }
        return null;
    }

    private Result<List<TimedEvent>, Unit> tryApply(final Path path, final int index, final List<ParsedKeyframe<?>> keyframes, final double time, final ModelRenderState state, final AnimationContext context) {
        if (path.path.length == index) {
            final var folder = Result.<TimedEvent>mutableFold();
            for (final ParsedKeyframe<?> keyframe : keyframes) {
                folder.accept(tryApply(keyframe, state, time, context));
            }
            return folder.get();
        }
        final Optional<ModelRenderState> child = state.getChild(path.path[index], time);
        if (child.isEmpty()) {
            return Result.failure(Unit.INSTANCE);
        }
        return tryApply(path, index + 1, keyframes, time, state, context);
    }

    private <T> Result<TimedEvent, Unit> tryApply(final ParsedKeyframe<T> keyframe, final ModelRenderState state, final double time, final AnimationContext context) {
        final double start = time + keyframe.offset;
        return state.getProperty(keyframe.property).reserve(keyframe.modifier.delay(time), start, start + keyframe.length, Easing.CONSTANT_1, context, keyframe.level);
    }

    private record Path(String... path) {
    }

    public static final class Builder {
        private final Map<Path, Map<PropertyKey<?>, List<KeyframeEntry<?>>>> entries = new Object2ReferenceOpenHashMap<>();

        public <T> Builder add(final List<String> path, final PropertyKey<T> key, final T value, final double time, final EasingFunction function, final Property.ReservationLevel level) {
            final Path p = new Path(path.toArray(new String[0]));
            entries.computeIfAbsent(p, k -> new Object2ReferenceOpenHashMap<>()).computeIfAbsent(key, k -> new ArrayList<>()).add(new KeyframeEntry<T>(key, value, time, function, level));
            return this;
        }

        public Animation<ModelRenderState> build() {
            final Map<Path, ModelEntry> map = new Object2ReferenceOpenHashMap<>();
            for (final Map.Entry<Path, Map<PropertyKey<?>, List<KeyframeEntry<?>>>> entry : entries.entrySet()) {
                map.put(entry.getKey(), buildModel(entry.getValue()));
            }
            return new ModelKeyframeAnimation(map);
        }

        private ModelEntry buildModel(final Map<PropertyKey<?>, List<KeyframeEntry<?>>> map) {
            final List<ParsedKeyframe<?>> keyframes = new ArrayList<>(map.size());
            for (final Map.Entry<PropertyKey<?>, List<KeyframeEntry<?>>> entry : map.entrySet()) {
                keyframes.addAll(build(entry.getKey(), entry.getValue()));
            }
            return new ModelEntry(List.copyOf(keyframes));
        }

        private <T> List<ParsedKeyframe<T>> build(final PropertyKey<T> property, final List<KeyframeEntry<?>> entries) {
            entries.sort(KeyframeEntry.COMPARATOR);
            final double start = entries.get(0).time;
            final double end = entries.get(entries.size() - 1).time;
            final Map<Property.ReservationLevel, List<OffsetEntry<T>>> offsetEntries = parse(entries);
            final List<ParsedKeyframe<T>> result = new ArrayList<>();
            for (final Map.Entry<Property.ReservationLevel, List<OffsetEntry<T>>> entry : offsetEntries.entrySet()) {
                final List<OffsetEntry<T>> list = entry.getValue();
                result.add(new ParsedKeyframe<>(property, time -> {
                    int index = 0;
                    while (index < list.size() && list.get(index).time <= time) {
                        index++;
                    }
                    if (index == 0) {
                        return list.get(0).value;
                    } else if (index == list.size()) {
                        return list.get(list.size() - 1).value;
                    }
                    final OffsetEntry<T> prev1 = list.get(index - 1);
                    final OffsetEntry<T> next = list.get(index);
                    return property.type().interpolator().interpolate(prev1.value, next.value, next.easing.ease(time));
                }, Easing.CONSTANT_1, end - start, start, entry.getKey()));
            }
            return result;
        }

        private <T> Map<Property.ReservationLevel, List<OffsetEntry<T>>> parse(final List<KeyframeEntry<?>> entries) {
            final Map<Property.ReservationLevel, List<OffsetEntry<T>>> offsetEntries = new EnumMap<>(Property.ReservationLevel.class);
            final Map<Property.ReservationLevel, KeyframeEntry<?>> prevs = new EnumMap<>(Property.ReservationLevel.class);
            for (final KeyframeEntry<?> entry : entries) {
                final KeyframeEntry<?> prev = prevs.get(entry.level);
                final OffsetEntry<T> offsetEntry;
                if (prev == null) {
                    offsetEntry = new OffsetEntry<>((T) entry.value, entry.time, 0, t -> 1);
                } else {
                    final double length = entry.time - prev.time;
                    final double time = entry.time;
                    final Easing easing = Easing.from(entry.function, prev.time, time);
                    offsetEntry = new OffsetEntry<T>((T) entry.value, time, length, easing);
                }
                offsetEntries.computeIfAbsent(entry.level, i -> new ArrayList<>()).add(offsetEntry);
                prevs.put(entry.level, entry);
            }
            return offsetEntries;
        }
    }

    private record OffsetEntry<T>(T value, double time, double length, Easing easing) {
    }

    private record KeyframeEntry<T>(PropertyKey<T> property, T value, double time, EasingFunction function,
                                    Property.ReservationLevel level) {
        private static final Comparator<KeyframeEntry<?>> COMPARATOR = Comparator.comparingDouble(KeyframeEntry::time);
    }

    private record ModelEntry(List<ParsedKeyframe<?>> keyframes) {

    }

    private record ParsedKeyframe<T>(
            PropertyKey<T> property,
            StateModifier<T> modifier,
            Easing easing,
            double length,
            double offset,
            Property.ReservationLevel level
    ) {
    }
}
