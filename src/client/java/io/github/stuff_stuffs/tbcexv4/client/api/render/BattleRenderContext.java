package io.github.stuff_stuffs.tbcexv4.client.api.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;

public interface BattleRenderContext {
    WorldRenderContext parent();

    int light(double x, double y, double z);

    MatrixStack lightMatrices();
}
