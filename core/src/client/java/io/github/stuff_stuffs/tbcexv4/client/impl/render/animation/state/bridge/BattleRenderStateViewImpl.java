package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.bridge;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.BattleEffectRenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.BattleRenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.ParticipantRenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge.RenderStateView;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.property.PropertyImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.BattleEffectRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.BattleRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.ParticipantRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public class BattleRenderStateViewImpl extends BattleRenderStateImpl implements BattleRenderStateView {
    private double lastUpdateTime = 0;

    @Override
    public Optional<ParticipantRenderStateView> getParticipant(final BattleParticipantHandle handle) {
        return getParticipant(handle, lastUpdateTime).map(c -> (ParticipantRenderStateViewImpl) c);
    }

    @Override
    protected ParticipantRenderStateImpl createParticipant(final BattleParticipantHandle k) {
        return new ParticipantRenderStateViewImpl(k, this, lastUpdateTime);
    }

    @Override
    protected BattleEffectRenderStateImpl createEffect(final Identifier k) {
        return new BattleEffectRenderStateViewImpl(k, this, lastUpdateTime);
    }

    @Override
    public int update(final double time) {
        lastUpdateTime = time;
        return super.update(time);
    }

    @Override
    public Set<BattleParticipantHandle> participants() {
        return participants(lastUpdateTime);
    }

    @Override
    public void addParticipant(final BattleParticipantHandle handle) {
        participantContainer.forceAdd(handle, lastUpdateTime);
    }

    @Override
    public void removeParticipant(final BattleParticipantHandle handle, final Property.@Nullable ReservationLevel level) {
        participantContainer.forceRemove(handle, lastUpdateTime, level);
    }

    @Override
    public Optional<BattleEffectRenderStateView> getEffect(final Identifier id) {
        return getEffect(id, lastUpdateTime).map(c -> (BattleEffectRenderStateView) c);
    }

    @Override
    public Set<Identifier> effects() {
        return effects(lastUpdateTime);
    }

    @Override
    public void addEffect(final Identifier id) {
        effectContainer.forceAdd(id, lastUpdateTime);
    }

    @Override
    public void removeEffect(final Identifier id, final Property.@Nullable ReservationLevel level) {
        effectContainer.forceRemove(id, lastUpdateTime, level);
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
        return null;
    }
}
