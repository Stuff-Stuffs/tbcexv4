package io.github.stuff_stuffs.tbcexv4.client.api.render.animation;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyType;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

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

        default <T0> StateModifier<T0> map(Function<T, T0> function) {
            return time -> function.apply(StateModifier.this.value(time));
        }

        default StateModifier<T> stretch(double speed) {
            return time -> this.value(time*speed);
        }

        static StateModifier<Unit> lock() {
            return time -> Unit.INSTANCE;
        }

        static <T, K extends Comparable<? super K>> StateModifier<T> split(List<K> list, DoubleFunction<K> keyFactory, ToDoubleFunction<K> timestampGetter, Function<K, T> extractor, PropertyType.Interpolator<T> interpolator) {
            if(list.isEmpty()) {
                throw new RuntimeException();
            }
            return time -> {
                K key = keyFactory.apply(time);
                int index = Collections.binarySearch(list, key);
                if(index>=0) {
                    return extractor.apply(list.get(index));
                }
                int rIndex = -index-1;
                if(rIndex==0) {
                    return extractor.apply(list.get(0));
                }
                if(rIndex==list.size()) {
                    return extractor.apply(list.get(list.size()-1));
                }
                K prev = list.get(rIndex-1);
                K next = list.get(rIndex-1);
                double prevTime = timestampGetter.applyAsDouble(prev);
                double nextTime = timestampGetter.applyAsDouble(next);
                double t = time-prevTime;
                double prog = t/(nextTime-prevTime);
                return interpolator.interpolate(extractor.apply(prev), extractor.apply(next), prog);
            };
        }


        static <T,K> StateModifier<T> split(List<K> list, DoubleFunction<K> keyFactory, ToDoubleFunction<K> timestampGetter, Function<K, T> extractor, PropertyType.Interpolator<T> interpolator, Comparator<K> comparator) {
            if(list.isEmpty()) {
                throw new RuntimeException();
            }
            return time -> {
                K key = keyFactory.apply(time);
                int index = Collections.binarySearch(list, key, comparator);
                if(index>=0) {
                    return extractor.apply(list.get(index));
                }
                int rIndex = -index-1;
                if(rIndex==0) {
                    return extractor.apply(list.get(0));
                }
                if(rIndex==list.size()) {
                    return extractor.apply(list.get(list.size()-1));
                }
                K prev = list.get(rIndex-1);
                K next = list.get(rIndex);
                double prevTime = timestampGetter.applyAsDouble(prev);
                double nextTime = timestampGetter.applyAsDouble(next);
                double t = time-prevTime;
                double prog = t/(nextTime-prevTime);
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
