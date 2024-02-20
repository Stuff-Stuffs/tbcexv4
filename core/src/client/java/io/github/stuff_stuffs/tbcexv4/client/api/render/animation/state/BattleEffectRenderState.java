package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyTypes;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.BattleEffectRenderer;
import net.minecraft.util.Identifier;

public non-sealed interface BattleEffectRenderState extends RenderState {
    PropertyKey<BattleEffectRenderer> RENDERER = new PropertyKey<>("renderer", PropertyTypes.BATTLE_EFFECT_RENDERER);

    Identifier id();

    @Override
    BattleRenderState parent();
}
