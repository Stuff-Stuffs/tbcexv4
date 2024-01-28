package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;

public class ParticipantRenderStateImpl extends RenderStateImpl implements ParticipantRenderState {
    private final BattleParticipantHandle id;
    private final ModelRenderStateImpl rootModel;
    private final BattleRenderState parent;

    public ParticipantRenderStateImpl(BattleParticipantHandle id, final BattleRenderState parent) {
        this.id = id;
        this.parent = parent;
        rootModel = new ModelRenderStateImpl("", this);
    }

    @Override
    public void update(final double time) {
        super.update(time);
        rootModel.update(time);
    }

    @Override
    public BattleParticipantHandle id() {
        return id;
    }

    @Override
    public ModelRenderState modelRoot() {
        return rootModel;
    }

    @Override
    public BattleRenderState parent() {
        return parent;
    }

    @Override
    public void cleanup(final AnimationContext context, final double time) {
        super.cleanup(context, time);
        rootModel.cleanup(context, time);
    }
}
