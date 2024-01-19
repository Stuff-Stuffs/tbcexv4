package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;

import java.util.Set;

public interface BattleRenderState extends RenderState {
    ParticipantRenderState getParticipant(BattleParticipantHandle handle);

    Set<BattleParticipantHandle> participants();

    void addParticipant(BattleParticipantHandle handle);

    void removeParticipant(BattleParticipantHandle handle);

    void setTime(double time);
}
