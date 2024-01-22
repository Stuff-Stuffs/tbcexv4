package io.github.stuff_stuffs.tbcexv4.client.api.render.animation;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public interface Animation<T extends RenderState> {
    Result<List<TimedEvent>, Unit> animate(double time, T state, AnimationContext context);

    interface StateModifier<T> {
        T value(double time);

        default StateModifier<T> delay(final double delay) {
            return time -> value(time + delay);
        }

        static StateModifier<Unit> lock() {
            return time -> Unit.INSTANCE;
        }
    }

    @ApiStatus.NonExtendable
    interface TimedEvent {
        double start();

        double end();
    }
}
