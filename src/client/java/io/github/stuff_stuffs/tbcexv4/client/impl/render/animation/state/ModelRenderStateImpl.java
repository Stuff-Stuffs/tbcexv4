package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ModelRenderStateImpl extends RenderStateImpl implements ModelRenderState {
    private final Map<String, ModelRenderStateImpl> children = new Object2ReferenceOpenHashMap<>();

    @Override
    public void update(final double time) {
        super.update(time);
        for (final ModelRenderStateImpl child : children.values()) {
            child.update(time);
        }
    }

    @Override
    public ModelRenderState getChild(final String id) {
        return children.get(id);
    }

    @Override
    public void addChild(final String id) {
        children.computeIfAbsent(id, i -> new ModelRenderStateImpl());
    }

    @Override
    public void removeChild(final String id) {
        children.remove(id);
    }

    @Override
    public Set<String> children() {
        return Collections.unmodifiableSet(children.keySet());
    }
}
