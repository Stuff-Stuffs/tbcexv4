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
    private final TimedContainer<BattleParticipantHandle, ParticipantRenderStateImpl> participantContainer;
    private final TimedContainer<Identifier, BattleEffectRenderStateImpl> effectContainer;
    private final List<ParticipantRenderState> cachedParticipants;
    private final List<BattleEffectRenderState> cachedEffects;

    public BattleRenderStateImpl() {
        participantContainer = new TimedContainer<>(k -> new ParticipantRenderStateImpl(this));
        effectContainer = new TimedContainer<>(k -> new BattleEffectRenderStateImpl(this));
        cachedParticipants = new ArrayList<>();
        cachedEffects = new ArrayList<>();
    }

    @Override
    public void update(final double time) {
        super.update(time);
        cachedParticipants.clear();
        cachedEffects.clear();
        participantContainer.update(time);
        effectContainer.update(time);
        for (final BattleParticipantHandle handle : participantContainer.children(time)) {
            cachedParticipants.add(participantContainer.get(handle, time));
        }
        for (final Identifier child : effectContainer.children(time)) {
            cachedEffects.add(effectContainer.get(child, time));
        }
    }

    @Override
    public void cleanup(final AnimationContext context, final double time) {
        super.cleanup(context, time);
        participantContainer.clear(context, time);
    }

    public List<ParticipantRenderState> cachedParticipants() {
        return cachedParticipants;
    }

    public List<BattleEffectRenderState> cachedEffects() {
        return cachedEffects;
    }

    @Override
    public @Nullable Optional<ParticipantRenderState> getParticipant(final BattleParticipantHandle handle, final double time) {
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
            final double last = state.lastAbove(Property.ReservationLevel.IDLE);
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
            final double last = state.lastAbove(Property.ReservationLevel.IDLE);
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
