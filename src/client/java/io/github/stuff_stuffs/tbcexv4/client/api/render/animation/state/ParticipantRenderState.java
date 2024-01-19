package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

import java.util.ArrayList;
import java.util.List;

public interface ParticipantRenderState extends RenderState {
    String POS_ID = "pos";

    ModelRenderState modelRoot();

    static Animation<BattleRenderState> lift(final Animation<ParticipantRenderState> animation, final BattleParticipantHandle handle) {
        return new Animation<>() {
            @Override
            public Result<List<AppliedStateModifier<?>>, Unit> setup(final double time, final BattleRenderState state, final AnimationContext context) {
                final ParticipantRenderState participant = state.getParticipant(handle);
                if (participant != null) {
                    return animation.setup(time, participant, context);
                } else {
                    return new Result.Failure<>(Unit.INSTANCE);
                }
            }

            @Override
            public void cleanupFailure(final double time, final BattleRenderState state, final AnimationContext context) {
                final ParticipantRenderState participant = state.getParticipant(handle);
                if (participant != null) {
                    animation.cleanupFailure(time, participant, context);
                }
            }
        };
    }

    static Animation<BattleRenderState> lift(final Animation<ParticipantRenderState> animation, final LiftingPredicate<ParticipantRenderState, BattleParticipantHandle> predicate) {
        return new Animation<>() {
            @Override
            public Result<List<AppliedStateModifier<?>>, Unit> setup(final double time, final BattleRenderState state, final AnimationContext context) {
                final List<AppliedStateModifier<?>> modifiers = new ArrayList<>();
                for (final BattleParticipantHandle handle : state.participants()) {
                    final ParticipantRenderState participant = state.getParticipant(handle);
                    if (predicate.test(participant, handle, context)) {
                        final Result<List<AppliedStateModifier<?>>, Unit> setup = animation.setup(time, participant, context);
                        if (setup instanceof final Result.Success<List<AppliedStateModifier<?>>, Unit> success) {
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
            }

            @Override
            public void cleanupFailure(final double time, final BattleRenderState state, final AnimationContext context) {
                for (final BattleParticipantHandle handle : state.participants()) {
                    final ParticipantRenderState participant = state.getParticipant(handle);
                    if (predicate.test(participant, handle, context)) {
                        animation.cleanupFailure(time, participant, context);
                    }
                }
            }
        };
    }
}
