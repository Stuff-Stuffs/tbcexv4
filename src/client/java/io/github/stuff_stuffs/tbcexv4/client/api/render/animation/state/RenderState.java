package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import org.jetbrains.annotations.Nullable;

public interface RenderState {
    PropertyKey<Unit> LOCK = new PropertyKey<>("lock", PropertyTypes.LOCK);

    <T> Property<T> getProperty(PropertyKey<T> key);

    @Nullable RenderState parent();

    default Result<Animation.TimedEvent, Unit> lock(final double start, final double end, final AnimationContext context, final Property.ReservationLevel level) {
        return getProperty(LOCK).reserve(Animation.StateModifier.lock(), start, end, t -> 1, context, level);
    }

    default Result<Animation.TimedEvent, Unit> completeLock(final double start, final double end, final AnimationContext context) {
        final Property<Unit> property = getProperty(LOCK);
        return property.reserve(Animation.StateModifier.lock(), start, end, t -> 1, context, Property.ReservationLevel.IDLE).flatmapSuccess(
                s0 -> property.reserve(Animation.StateModifier.lock(), start, end, t -> 1, context, Property.ReservationLevel.ACTION).flatmapSuccess(
                        s1 -> property.reserve(Animation.StateModifier.lock(), start, end, t -> 1, context, Property.ReservationLevel.EFFECT).flatmapSuccess(
                                s2 -> property.reserve(Animation.StateModifier.lock(), start, end, t -> 1, context, Property.ReservationLevel.TRANSITION)
                        )
                )
        );
    }

    interface LiftingPredicate<T extends RenderState, I> {
        boolean test(T state, I id, AnimationContext context);
    }
}
