package io.github.stuff_stuffs.tbcexv4.client.api.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;

public interface BattleRenderContext {
    WorldRenderContext parent();

    default int light(final double x, final double y, final double z) {
        return light(x, y, z, 0);
    }

    int light(double x, double y, double z, int emissionLight);

    default void light(double x0, double y0, double z0, double x1, double y1, double z1, LightResult result) {
        light(x0,y0, z0, x1, y1, z1, 0, result);
    }

    void light(double x0, double y0, double z0, double x1, double y1, double z1, int emissionLight, LightResult result);

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
