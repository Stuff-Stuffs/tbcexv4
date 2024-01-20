package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

public interface Property<T> {
    PropertyType<T> type();

    Result<Animation.TimedEvent, Unit> reserve(Animation.StateModifier<T> modifier, double startTime, double endTime, Easing inout, AnimationContext context, ReservationLevel level);

    Result<Animation.TimedEvent, Unit> setDefaultValue(T val, double time, AnimationContext context);

    void clearAll(AnimationContext context);

    T get();

    enum ReservationLevel {
        IDLE,
        ACTION,
        EFFECT,
        TRANSITION
    }
}
