package io.github.stuff_stuffs.tbcexv4.client.internal;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface BattleDebugRenderer {
    void render(WorldRenderContext context, Battle battle);
}
