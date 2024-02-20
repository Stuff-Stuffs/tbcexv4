package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.bridge;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.ModelRenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.RenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.property.PropertyImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.ModelRenderStateImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class ModelRenderStateViewImpl extends ModelRenderStateImpl implements ModelRenderStateView {
    private double lastUpdateTime;

    public ModelRenderStateViewImpl(final String id, final RenderState parent, final double lastUpdateTime) {
        super(id, parent);
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    protected ModelRenderStateImpl createChild(final String k) {
        return new ModelRenderStateViewImpl(k, this, lastUpdateTime);
    }

    @Override
    public int update(final double time) {
        lastUpdateTime = time;
        return super.update(time);
    }

    @Override
    public Optional<ModelRenderStateView> getChild(final String id) {
        return getChild(id, lastUpdateTime).map(c -> (ModelRenderStateViewImpl) c);
    }

    @Override
    public void addChild(final String id) {
        timedContainer.forceAdd(id, lastUpdateTime);
    }

    @Override
    public void removeChild(final String id, final Property.@Nullable ReservationLevel level) {
        timedContainer.forceRemove(id, lastUpdateTime, level);
    }

    @Override
    public Set<String> children() {
        return children(lastUpdateTime);
    }

    @Override
    public <T> void set(final PropertyKey<T> key, final T value) {
        final PropertyImpl<?> old = map.get(key);
        final PropertyImpl<T> current;
        if (old != null && !old.isBridge()) {
            map.remove(key);
            list.remove(old);
        }

        if (old == null || !old.isBridge()) {
            current = new PropertyImpl<>(key.type(), value, true);
            map.put(key, current);
            list.add(current);
        } else {
            //noinspection unchecked
            current = (PropertyImpl<T>) old;
        }
        current.setBridgeValue(value);
    }

    @Override
    public @Nullable RenderStateView parentView() {
        return (RenderStateView) parent;
    }
}
