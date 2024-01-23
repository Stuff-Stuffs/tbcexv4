package io.github.stuff_stuffs.tbcexv4.client.api.render.renderer;

import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleRenderContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleEffectRenderState;

public interface BattleEffectRenderer {
    void render(BattleRenderContext context, BattleEffectRenderState state);
}
