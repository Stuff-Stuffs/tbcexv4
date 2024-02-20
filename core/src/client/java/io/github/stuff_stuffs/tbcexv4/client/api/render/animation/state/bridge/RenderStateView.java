package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import org.jetbrains.annotations.Nullable;

public interface RenderStateView {
    <T> void set(PropertyKey<T> key, T value);

    @Nullable RenderStateView parentView();
}
