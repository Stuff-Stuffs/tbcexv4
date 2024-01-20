package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

public abstract class RenderStateImpl implements RenderState {
    private final Reference2ObjectMap<PropertyKey<?>, PropertyImpl<?>> map;

    public RenderStateImpl() {
        map = new Reference2ObjectOpenHashMap<>();
    }

    public void cleanup(final AnimationContext context) {
        for (final PropertyImpl<?> property : map.values()) {
            property.clearAll(context);
        }
    }

    public double lastAbove(@Nullable final Property.ReservationLevel level) {
        double last = Double.NEGATIVE_INFINITY;
        for (final PropertyImpl<?> property : map.values()) {
            last = Math.max(last, property.lastAbove(level));
        }
        return last;
    }

    public void update(final double time) {
        for (final PropertyImpl<?> property : map.values()) {
            property.compute(time);
        }
    }

    @Override
    public <T> Property<T> getProperty(final PropertyKey<T> key) {
        //noinspection unchecked
        PropertyImpl<T> property = (PropertyImpl<T>) map.get(key);
        if (property != null) {
            return property;
        }
        property = new PropertyImpl<>(key.type(), key.type().defaultValue().get());
        map.put(key, property);
        return property;
    }
}
