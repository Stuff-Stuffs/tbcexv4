package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

import java.util.Optional;
import java.util.Set;

public interface BattleRenderState extends RenderState {
    Optional<ParticipantRenderState> getParticipant(BattleParticipantHandle handle, double time);

    Set<BattleParticipantHandle> participants(double time);

    Result<Animation.TimedEvent, Unit> addParticipant(BattleParticipantHandle handle, double time, AnimationContext context);

    Result<Animation.TimedEvent, Unit> removeParticipant(BattleParticipantHandle handle, double time, AnimationContext context);

    void clearEvents(AnimationContext context);
}
