package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;

public class ParticipantRenderStateImpl extends RenderStateImpl implements ParticipantRenderState {
    private final ModelRenderStateImpl rootModel;
    private final BattleRenderState parent;

    public ParticipantRenderStateImpl(final BattleRenderState parent) {
        this.parent = parent;
        rootModel = new ModelRenderStateImpl(this);
    }

    @Override
    public void update(final double time) {
        super.update(time);
        rootModel.update(time);
    }

    @Override
    public ModelRenderState modelRoot() {
        return rootModel;
    }

    @Override
    public BattleRenderState parent() {
        return parent;
    }
}
