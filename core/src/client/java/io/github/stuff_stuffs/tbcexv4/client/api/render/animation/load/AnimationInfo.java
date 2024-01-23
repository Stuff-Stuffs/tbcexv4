package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.load;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyType;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;

import java.util.Map;

public interface AnimationInfo<T extends RenderState> {
    Iterable<Parameter<?>> parameters();

    Animation<T> create(Map<String, Object> parameters);

    interface Parameter<T> {
        String id();

        PropertyType<T> type();
    }

    interface Created<T extends RenderState> {
        Animation<T> animation();

        double length();
    }
}
