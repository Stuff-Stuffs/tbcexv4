package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ModelRenderStateImpl extends RenderStateImpl implements ModelRenderState {
    private final String id;
    private final TimedContainer<String, ModelRenderStateImpl> timedContainer;
    private final RenderState parent;
    private final List<ModelRenderState> cached;

    public ModelRenderStateImpl(final String id, final RenderState parent) {
        this.id = id;
        this.parent = parent;
        timedContainer = new TimedContainer<>(k -> new ModelRenderStateImpl(k, this));
        cached = new ArrayList<>();
    }

    @Override
    public void update(final double time) {
        super.update(time);
        timedContainer.update(time);
        cached.clear();
        for (final String id : timedContainer.children(time)) {
            cached.add(timedContainer.get(id, time));
        }
    }

    @Override
    public void cleanup(final AnimationContext context, final double time) {
        super.cleanup(context, time);
        timedContainer.clear(context, time);
    }

    public List<ModelRenderState> cached() {
        return cached;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Optional<ModelRenderState> getChild(final String id, final double time) {
        return Optional.ofNullable(timedContainer.get(id, time));
    }

    @Override
    public Result<Animation.TimedEvent, Unit> addChild(final String id, final double time, final AnimationContext context) {
        return timedContainer.add(id, time, context);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> removeChild(final String id, final double time, final AnimationContext context) {
        final ModelRenderStateImpl state = timedContainer.get(id, time);
        if (state != null) {
            final double last = state.lastAbove(Property.ReservationLevel.IDLE);
            if (last > time) {
                return Result.failure(Unit.INSTANCE);
            }
        }
        return timedContainer.remove(id, time, context);
    }

    @Override
    public Set<String> children(final double time) {
        return timedContainer.children(time);
    }

    @Override
    public List<ModelRenderState> getChildren(final String id, final double time) {
        final List<ModelRenderState> list = new ArrayList<>();
        for (final String child : children(time)) {
            final Optional<ModelRenderState> opt = getChild(child, time);
            if (opt.isEmpty()) {
                continue;
            }
            final ModelRenderState childState = opt.get();
            if (child.equals(id)) {
                list.add(childState);
            }
            list.addAll(childState.getChildren(id, time));
        }
        return list;
    }

    @Override
    public RenderState parent() {
        return parent;
    }
}
