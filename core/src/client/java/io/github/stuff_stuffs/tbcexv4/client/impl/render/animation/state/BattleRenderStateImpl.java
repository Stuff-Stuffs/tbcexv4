package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleEffectRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BattleRenderStateImpl extends RenderStateImpl implements BattleRenderState {
    protected final TimedContainer<BattleParticipantHandle, ParticipantRenderStateImpl> participantContainer;
    protected final TimedContainer<Identifier, BattleEffectRenderStateImpl> effectContainer;
    protected final List<ParticipantRenderState> cachedParticipants;
    protected final List<BattleEffectRenderState> cachedEffects;

    public BattleRenderStateImpl() {
        super();
        participantContainer = new TimedContainer<>(this::createParticipant);
        effectContainer = new TimedContainer<>(this::createEffect);
        cachedParticipants = new ArrayList<>();
        cachedEffects = new ArrayList<>();
    }

    protected ParticipantRenderStateImpl createParticipant(final BattleParticipantHandle k) {
        return new ParticipantRenderStateImpl(k, this);
    }

    protected BattleEffectRenderStateImpl createEffect(final Identifier k) {
        return new BattleEffectRenderStateImpl(k, this);
    }

    @Override
    public void clearUpTo(final double time) {
        super.clearUpTo(time);
        participantContainer.clearUpTo(time);
        effectContainer.clearUpTo(time);
    }

    @Override
    public void checkpoint() {
        super.checkpoint();
        participantContainer.checkpoint();
        effectContainer.checkpoint();
    }

    @Override
    public int update(final double time) {
        int flags = super.update(time);
        cachedParticipants.clear();
        cachedEffects.clear();
        flags |= participantContainer.update(time);
        flags |= effectContainer.update(time);
        for (final BattleParticipantHandle handle : participantContainer.children(time)) {
            cachedParticipants.add(participantContainer.get(handle, time));
        }
        for (final Identifier child : effectContainer.children(time)) {
            cachedEffects.add(effectContainer.get(child, time));
        }
        return flags;
    }

    @Override
    public void cleanup(final AnimationContext context, final double time) {
        super.cleanup(context, time);
        participantContainer.clear(context, time);
        effectContainer.clear(context, time);
    }

    @Override
    public double lastAbove(final double t, final Property.@Nullable ReservationLevel level) {
        double m = super.lastAbove(t, level);
        for (final BattleParticipantHandle child : participantContainer.children(t)) {
            final double v = participantContainer.get(child, t).lastAbove(t, level);
            m = Math.max(m, v);
        }
        for (final Identifier child : effectContainer.children(t)) {
            final double v = effectContainer.get(child, t).lastAbove(t, level);
            m = Math.max(m, v);
        }
        return m;
    }

    public List<ParticipantRenderState> cachedParticipants() {
        return cachedParticipants;
    }

    public List<BattleEffectRenderState> cachedEffects() {
        return cachedEffects;
    }

    @Override
    public Optional<ParticipantRenderState> getParticipant(final BattleParticipantHandle handle, final double time) {
        return Optional.ofNullable(participantContainer.get(handle, time));
    }

    @Override
    public Set<BattleParticipantHandle> participants(final double time) {
        return participantContainer.children(time);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> addParticipant(final BattleParticipantHandle handle, final double time, final AnimationContext context) {
        return participantContainer.add(handle, time, context);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> removeParticipant(final BattleParticipantHandle handle, final double time, final AnimationContext context) {
        final ParticipantRenderStateImpl state = participantContainer.get(handle, time);
        if (state != null) {
            final double last = state.lastAbove(time, Property.ReservationLevel.IDLE);
            if (last > time) {
                return Result.failure(Unit.INSTANCE);
            }
        }
        return participantContainer.remove(handle, time, context);
    }

    @Override
    public Optional<BattleEffectRenderState> getEffect(final Identifier id, final double time) {
        return Optional.ofNullable(effectContainer.get(id, time));
    }

    @Override
    public Set<Identifier> effects(final double time) {
        return effectContainer.children(time);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> addEffect(final Identifier id, final double time, final AnimationContext context) {
        return effectContainer.add(id, time, context);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> removeEffect(final Identifier id, final double time, final AnimationContext context) {
        final BattleEffectRenderStateImpl state = effectContainer.get(id, time);
        if (state != null) {
            final double last = state.lastAbove(time, Property.ReservationLevel.IDLE);
            if (last > time) {
                return Result.failure(Unit.INSTANCE);
            }
        }
        return effectContainer.remove(id, time, context);
    }

    @Override
    public @Nullable RenderState parent() {
        return null;
    }
}
