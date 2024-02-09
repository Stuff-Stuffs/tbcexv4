package io.github.stuff_stuffs.tbcexv4.client.api.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;

public interface BattleRenderContext {
    WorldRenderContext parent();

    int light(double x, double y, double z);

    void light(double x0, double y0, double z0, double x1, double y1, double z1, LightResult result);

    MatrixStack lightMatrices();

    final class LightResult {
        public int l0;
        public int l1;
        public int l2;
        public int l3;
        public int l4;
        public int l5;
        public int l6;
        public int l7;
    }
}
