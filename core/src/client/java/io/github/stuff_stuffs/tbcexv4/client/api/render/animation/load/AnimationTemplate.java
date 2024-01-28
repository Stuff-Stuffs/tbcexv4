package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.load;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;

import java.util.Set;

public interface AnimationTemplate<T extends RenderState> {
    Animation<T> apply(ParameterMap parameters);

    Iterable<Parameter<?>> requiredParameters();

    Iterable<Parameter<?>> optionalParameters();

    record Parameter<T>(String id, Class<T> type) {
    }

    interface ParameterMap {
        Set<Parameter<?>> keys();

        <T> T get(Parameter<T> parameter);
    }
}
