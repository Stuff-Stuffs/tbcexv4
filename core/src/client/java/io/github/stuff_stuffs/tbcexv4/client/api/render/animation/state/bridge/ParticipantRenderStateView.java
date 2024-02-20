package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;

public interface ParticipantRenderStateView extends RenderStateView {
    BattleParticipantHandle id();

    ModelRenderStateView modelRootView();

    @Override
    BattleRenderStateView parentView();
}
