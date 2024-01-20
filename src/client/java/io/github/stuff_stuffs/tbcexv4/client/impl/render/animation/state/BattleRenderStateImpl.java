package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BattleRenderStateImpl extends RenderStateImpl implements BattleRenderState {
    private final TimedContainer<BattleParticipantHandle, ParticipantRenderStateImpl> timedContainer;
    private final List<ParticipantRenderState> cached;

    public BattleRenderStateImpl() {
        timedContainer = new TimedContainer<>(k -> new ParticipantRenderStateImpl(this));
        cached = new ArrayList<>();
    }

    @Override
    public void update(final double time) {
        super.update(time);
        timedContainer.update(time);
        cached.clear();
        for (final BattleParticipantHandle handle : timedContainer.children(time)) {
            cached.add(timedContainer.get(handle, time));
        }
    }

    @Override
    public void cleanup(final AnimationContext context) {
        super.cleanup(context);
        timedContainer.clear(context);
    }

    public List<ParticipantRenderState> cached() {
        return cached;
    }

    @Override
    public @Nullable Optional<ParticipantRenderState> getParticipant(final BattleParticipantHandle handle, final double time) {
        return Optional.ofNullable(timedContainer.get(handle, time));
    }

    @Override
    public Set<BattleParticipantHandle> participants(final double time) {
        return timedContainer.children(time);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> addParticipant(final BattleParticipantHandle handle, final double time, final AnimationContext context) {
        return timedContainer.add(handle, time, context);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> removeParticipant(final BattleParticipantHandle handle, final double time, final AnimationContext context) {
        final ParticipantRenderStateImpl state = timedContainer.get(handle, time);
        if (state != null) {
            final double last = state.lastAbove(Property.ReservationLevel.IDLE);
            if (last > time) {
                return Result.failure(Unit.INSTANCE);
            }
        }
        return timedContainer.remove(handle, time, context);
    }

    @Override
    public void clearEvents(final AnimationContext context) {
        timedContainer.clear(context);
    }

    @Override
    public @Nullable RenderState parent() {
        return null;
    }
}
