package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public interface ModelRenderStateView extends RenderStateView {
    Optional<ModelRenderStateView> getChild(String id);

    void addChild(String id);

    void removeChild(String id, Property.@Nullable ReservationLevel level);

    Set<String> children();
}
