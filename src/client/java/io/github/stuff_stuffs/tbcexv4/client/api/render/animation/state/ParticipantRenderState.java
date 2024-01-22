package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface ParticipantRenderState extends RenderState {
    PropertyKey<Vec3d> POSITION = ModelRenderState.POSITION;

    ModelRenderState modelRoot();

    @Override
    BattleRenderState parent();

    static Animation<BattleRenderState> lift(final Animation<ParticipantRenderState> animation, final BattleParticipantHandle handle) {
        return (time, state, context) -> {
            final Optional<ParticipantRenderState> participant = state.getParticipant(handle, time);
            if (participant.isPresent()) {
                return animation.animate(time, participant.get(), context);
            } else {
                return new Result.Failure<>(Unit.INSTANCE);
            }
        };
    }

    static Animation<BattleRenderState> lift(final Animation<ParticipantRenderState> animation, final LiftingPredicate<ParticipantRenderState, BattleParticipantHandle> predicate) {
        return (time, state, context) -> {
            final List<Animation.TimedEvent> modifiers = new ArrayList<>();
            for (final BattleParticipantHandle handle : state.participants(time)) {
                final Optional<ParticipantRenderState> participant = state.getParticipant(handle, time);
                if (participant.isPresent() && predicate.test(participant.get(), handle, context)) {
                    final Result<List<Animation.TimedEvent>, Unit> setup = animation.animate(time, participant.get(), context);
                    if (setup instanceof final Result.Success<List<Animation.TimedEvent>, Unit> success) {
                        modifiers.addAll(success.val());
                    } else {
                        return setup;
                    }
                }
            }
            if (modifiers.isEmpty()) {
                return new Result.Failure<>(Unit.INSTANCE);
            }
            return new Result.Success<>(modifiers);
        };
    }
}
