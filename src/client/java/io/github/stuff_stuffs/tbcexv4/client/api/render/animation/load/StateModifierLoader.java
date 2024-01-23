package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.load;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyType;
import io.github.stuff_stuffs.tbcexv4.common.api.util.DualCodec;
import io.github.stuff_stuffs.tbcexv4.common.api.util.EasingFunction;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.*;

public final class StateModifierLoader {
    public static <T> Codec<SimpleStateModifier<T>> createSimpleCodec(final PropertyType<T> type, final EasingFunction fallback) {
        return Codec.unboundedMap(Codec.STRING.comapFlatMap(s -> {
            try {
                return DataResult.success(Double.parseDouble(s));
            } catch (final NumberFormatException e) {
                return DataResult.error(() -> s + " is not a valid double!");
            }
        }, d -> Double.toString(d)), new DualCodec<>(type.codec().flatComapMap(t -> Pair.of(t, fallback), pair -> {
            if (pair.getSecond() == fallback) {
                return DataResult.success(pair.getFirst());
            } else {
                return DataResult.error(() -> "Easing mismatch");
            }
        }), RecordCodecBuilder.create(instance -> instance.group(
                type.codec().fieldOf("value").forGetter(Pair::getFirst),
                EasingFunction.CODEC.fieldOf("easing").forGetter(Pair::getSecond)
        ).apply(instance, Pair::of)))).xmap(map -> {
            final List<Entry<T>> entries = new ArrayList<>(map.size());
            for (final Map.Entry<Double, Pair<T, EasingFunction>> entry : map.entrySet()) {
                entries.add(new Entry<>(entry.getValue().getFirst(), entry.getKey(), entry.getValue().getSecond()));
            }
            return new SimpleStateModifier<>(entries, type.interpolator(), 0);
        }, modifier -> {
            final Map<Double, Pair<T, EasingFunction>> map = new Object2ReferenceOpenHashMap<>();
            for (final Entry<T> entry : modifier.entries) {
                map.put(entry.time, Pair.of(entry.value, entry.easing));
            }
            return map;
        });
    }

    public static final class SimpleStateModifier<T> implements Animation.StateModifier<T> {
        private final List<Entry<T>> entries;
        private final PropertyType.Interpolator<T> interpolator;
        private final double delay;

        private SimpleStateModifier(final List<Entry<T>> entries, final PropertyType.Interpolator<T> interpolator, final double delay) {
            this.entries = new ArrayList<>(entries);
            this.interpolator = interpolator;
            this.delay = delay;
            this.entries.sort(Entry.COMPARATOR);
            if (entries.isEmpty()) {
                throw new RuntimeException();
            }
            if (entries.get(0).time < 0) {
                throw new RuntimeException();
            }
        }

        @Override
        public T value(final double time) {
            final Entry<T> search = new Entry<>(null, time - delay, EasingFunction.LINEAR);
            final int index = Collections.binarySearch(entries, search, Entry.COMPARATOR);
            if (index >= 0) {
                return entries.get(index).value;
            }
            final int rIndex = -index - 1;
            if (rIndex == 0) {
                return entries.get(0).value;
            }
            if (rIndex == entries.size()) {
                return entries.get(entries.size() - 1).value;
            }
            final int prev = Math.max(rIndex - 1, 0);
            final Entry<T> first = entries.get(prev);
            final Entry<T> second = entries.get(rIndex);
            final double t = (search.time - first.time) / (second.time - first.time);
            return interpolator.interpolate(first.value, second.value, second.easing.remap(t));
        }

        @Override
        public Animation.StateModifier<T> delay(final double delay) {
            return new SimpleStateModifier<>(entries, interpolator, delay + this.delay);
        }
    }

    private record Entry<T>(T value, double time, EasingFunction easing) {
        private static final Comparator<Entry<?>> COMPARATOR = Comparator.comparingDouble(Entry::time);
    }

    private StateModifierLoader() {
    }
}
