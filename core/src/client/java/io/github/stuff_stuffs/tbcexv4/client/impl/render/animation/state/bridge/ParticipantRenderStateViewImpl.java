package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.bridge;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.BattleRenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.ModelRenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.ParticipantRenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.property.PropertyImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.ModelRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.ParticipantRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;

public class ParticipantRenderStateViewImpl extends ParticipantRenderStateImpl implements ParticipantRenderStateView {
    private double lastUpdateTime;

    public ParticipantRenderStateViewImpl(final BattleParticipantHandle id, final BattleRenderState parent, final double lastUpdateTime) {
        super(id, parent);
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    protected ModelRenderStateImpl createRoot() {
        return new ModelRenderStateViewImpl("root", this, lastUpdateTime);
    }

    @Override
    public int update(final double time) {
        lastUpdateTime = time;
        return super.update(time);
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
    public ModelRenderStateView modelRootView() {
        return (ModelRenderStateView) modelRoot();
    }

    @Override
    public BattleRenderStateView parentView() {
        return (BattleRenderStateView) parent();
    }
}
