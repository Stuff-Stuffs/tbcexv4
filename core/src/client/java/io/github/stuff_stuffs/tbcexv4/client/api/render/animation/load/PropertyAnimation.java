package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.load;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

public class PropertyAnimation<T> {
    private final PropertyKey<T> key;
    private final StateModifierLoader.SimpleStateModifier<T> stateModifier;
    private final double offset;
    private final double length;
    private final Easing.SimpleEasing in;
    private final Easing.SimpleEasing out;
    private final Property.ReservationLevel level;

    public PropertyAnimation(final PropertyKey<T> key, final StateModifierLoader.SimpleStateModifier<T> modifier, final double offset, final double length, final Easing.SimpleEasing in, final Easing.SimpleEasing out, final Property.ReservationLevel level) {
        this.key = key;
        stateModifier = modifier;
        this.offset = offset;
        this.length = length;
        this.in = in;
        this.out = out;
        this.level = level;
    }

    public Result<Animation.TimedEvent, Unit> apply(final double time, final RenderState state, final AnimationContext context) {
        final double realTime = time + offset;
        final Easing.SimpleEasing in = this.in.delay(realTime);
        final Easing.SimpleEasing out = this.out.delay(realTime);
        return state.getProperty(key).reserve(stateModifier.delay(realTime), realTime, offset + time + length, t -> Math.min(in.ease(t), out.ease(t)), context, level);
    }
}
