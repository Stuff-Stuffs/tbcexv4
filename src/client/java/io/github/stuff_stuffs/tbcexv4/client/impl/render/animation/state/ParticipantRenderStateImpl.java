package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;

public class ParticipantRenderStateImpl extends RenderStateImpl implements ParticipantRenderState {
    private final ModelRenderStateImpl rootModel = new ModelRenderStateImpl();

    @Override
    public void update(final double time) {
        super.update(time);
        rootModel.update(time);
    }

    @Override
    public ModelRenderState modelRoot() {
        return rootModel;
    }
}
