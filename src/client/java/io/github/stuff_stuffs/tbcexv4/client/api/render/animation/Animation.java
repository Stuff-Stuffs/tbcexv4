package io.github.stuff_stuffs.tbcexv4.client.api.render.animation;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.PropertyType;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public interface Animation<T extends RenderState> {
    Result<List<AppliedStateModifier<?>>, Unit> setup(double time, T state, AnimationContext context);

    void cleanupFailure(double time, T state, AnimationContext context);

    interface StateModifier<T> {
        T value(double time);

        static StateModifier<Unit> lock() {
            return time -> Unit.INSTANCE;
        }
    }

    @ApiStatus.NonExtendable
    interface AppliedStateModifier<T> {
        double start();

        double end();

        Easing inout();

        StateModifier<T> modifier();

        String id();

        PropertyType<T> type();

        RenderState state();
    }
}
