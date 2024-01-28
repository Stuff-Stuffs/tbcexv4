package io.github.stuff_stuffs.tbcexv4.client.api.render.animation;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyType;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public interface Animation<T extends RenderState> {
    Result<List<TimedEvent>, Unit> animate(double time, T state, AnimationContext context);

    interface StateModifier<T> {
        T value(double time);

        default StateModifier<T> delay(final double delay) {
            return time -> value(time - delay);
        }

        default <T0> StateModifier<T0> map(final Function<T, T0> function) {
            return time -> function.apply(value(time));
        }

        default StateModifier<T> stretch(final double speed) {
            return time -> value(time * speed);
        }

        static StateModifier<Unit> lock() {
            return time -> Unit.INSTANCE;
        }

        static <T, K extends Comparable<? super K>> StateModifier<T> split(final List<K> list, final DoubleFunction<K> keyFactory, final ToDoubleFunction<K> timestampGetter, final Function<K, T> extractor, final PropertyType.Interpolator<T> interpolator) {
            if (list.isEmpty()) {
                throw new RuntimeException();
            }
            return time -> {
                final K key = keyFactory.apply(time);
                final int index = Collections.binarySearch(list, key);
                if (index >= 0) {
                    return extractor.apply(list.get(index));
                }
                final int rIndex = -index - 1;
                if (rIndex == 0) {
                    return extractor.apply(list.get(0));
                }
                if (rIndex == list.size()) {
                    return extractor.apply(list.get(list.size() - 1));
                }
                final K prev = list.get(rIndex - 1);
                final K next = list.get(rIndex - 1);
                final double prevTime = timestampGetter.applyAsDouble(prev);
                final double nextTime = timestampGetter.applyAsDouble(next);
                final double t = time - prevTime;
                final double prog = t / (nextTime - prevTime);
                return interpolator.interpolate(extractor.apply(prev), extractor.apply(next), prog);
            };
        }


        static <T, K> StateModifier<T> split(final List<K> list, final DoubleFunction<K> keyFactory, final ToDoubleFunction<K> timestampGetter, final Function<K, T> extractor, final PropertyType.Interpolator<T> interpolator, final Comparator<K> comparator) {
            if (list.isEmpty()) {
                throw new RuntimeException();
            }
            return time -> {
                final K key = keyFactory.apply(time);
                final int index = Collections.binarySearch(list, key, comparator);
                if (index >= 0) {
                    return extractor.apply(list.get(index));
                }
                final int rIndex = -index - 1;
                if (rIndex == 0) {
                    return extractor.apply(list.get(0));
                }
                if (rIndex == list.size()) {
                    return extractor.apply(list.get(list.size() - 1));
                }
                final K prev = list.get(rIndex - 1);
                final K next = list.get(rIndex);
                final double prevTime = timestampGetter.applyAsDouble(prev);
                final double nextTime = timestampGetter.applyAsDouble(next);
                final double t = time - prevTime;
                final double prog = t / (nextTime - prevTime);
                return interpolator.interpolate(extractor.apply(prev), extractor.apply(next), prog);
            };
        }
    }


    @ApiStatus.NonExtendable
    interface TimedEvent {
        double start();

        double end();
    }
}
