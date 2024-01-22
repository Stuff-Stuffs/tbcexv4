package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleEffectRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;

public class BattleEffectRenderStateImpl extends RenderStateImpl implements BattleEffectRenderState {
    private final BattleRenderState parent;

    public BattleEffectRenderStateImpl(final BattleRenderState parent) {
        this.parent = parent;
    }

    @Override
    public BattleRenderState parent() {
        return parent;
    }
}
