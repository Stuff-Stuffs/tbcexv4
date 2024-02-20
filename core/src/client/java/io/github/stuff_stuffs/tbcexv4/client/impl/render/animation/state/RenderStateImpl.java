package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.property.PropertyImpl;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class RenderStateImpl {
    protected final Map<PropertyKey<?>, PropertyImpl<?>> map;
    protected final List<PropertyImpl<?>> list = new ArrayList<>();

    public RenderStateImpl() {
        map = new Object2ReferenceLinkedOpenHashMap<>();
    }

    public void clearUpTo(final double time) {
        for (final PropertyImpl<?> value : map.values()) {
            value.clearUpTo(time);
        }
    }

    public void checkpoint() {
        for (int i = 0, size = list.size(); i < size; i++) {
            list.get(i).checkpoint();
        }
    }

    public void cleanup(final AnimationContext context, final double time) {
        for (int i = 0, size = list.size(); i < size; i++) {
            final PropertyImpl<?> property = list.get(i);
            property.clearAll(context);
        }
    }

    public double lastAbove(double t, @Nullable final Property.ReservationLevel level) {
        double last = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < list.size(); i++) {
            final PropertyImpl<?> property = list.get(i);
            last = Math.max(last, property.lastAbove(level));
        }
        return last;
    }

    public int update(final double time) {
        for (int i = 0, size = list.size(); i < size; i++) {
            final PropertyImpl<?> property = list.get(i);
            property.compute(time);
        }
        return 0;
    }

    public <T> Property<T> getProperty(final PropertyKey<T> key) {
        //noinspection unchecked
        PropertyImpl<T> property = (PropertyImpl<T>) map.get(key);
        if (property != null) {
            return property;
        }
        property = new PropertyImpl<>(key.type(), key.type().defaultValue().get());
        map.put(key, property);
        list.add(property);
        return property;
    }
}
