package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleEffectRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import net.minecraft.util.Identifier;

public class BattleEffectRenderStateImpl extends RenderStateImpl implements BattleEffectRenderState {
    private final Identifier id;
    private final BattleRenderState parent;

    public BattleEffectRenderStateImpl(final Identifier id, final BattleRenderState parent) {
        super();
        this.id = id;
        this.parent = parent;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public BattleRenderState parent() {
        return parent;
    }
}
