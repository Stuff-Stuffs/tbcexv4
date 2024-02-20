package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge;

import net.minecraft.util.Identifier;

public interface BattleEffectRenderStateView extends RenderStateView {
    Identifier id();

    @Override
    BattleRenderStateView parentView();
}
