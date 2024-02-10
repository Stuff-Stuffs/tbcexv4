package io.github.stuff_stuffs.tbcexv4.client.api.render.renderer;

import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleRenderContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.function.Function;

public class DefaultModelRenderer implements ModelRenderer {

    @Override
    public void render(final BattleRenderContext context, final ModelRenderState state) {
        final Optional<ModelRenderState.ModelData> opt = state.getProperty(ModelRenderState.MODEL_DATA).get();
        if (opt.isEmpty()) {
            return;
        }
        final ModelRenderState.ModelData modelData = opt.get();
        final Vec3d extents = modelData.extents();
        if (extents.lengthSquared() > 0.001) {
            final int color = state.getProperty(ModelRenderState.COLOR).get();
            final MatrixStack matrices = context.parent().matrixStack();
            final MatrixStack lightMatrices = context.lightMatrices();
            matrices.push();
            lightMatrices.push();
            applyTransformation(matrices, lightMatrices, state, state.getProperty(ModelRenderState.LAST_INVERSION).get());
            final Vec3d offset = modelData.offset();
            matrices.translate(offset.x, offset.y, offset.z);
            lightMatrices.translate(offset.x, offset.y, offset.z);
            final Matrix4f pMat = matrices.peek().getPositionMatrix();
            final Matrix3f nMat = matrices.peek().getNormalMatrix();
            final VertexConsumerProvider consumers = context.parent().consumers();
            final Optional<ModelRenderState.TextureData> data = state.getProperty(ModelRenderState.TEXTURE_DATA).get();
            if (data.isEmpty()) {
                final VertexConsumer buffer = consumers.getBuffer(RenderLayer.getDebugQuads());
                drawUntextured(buffer, extents, pMat, color);
            } else {
                final ModelRenderState.TextureData textureData = data.get();
                final Function<Identifier, RenderLayer> layerFactory = textureData.transparent() ? RenderLayer::getEntityTranslucent : RenderLayer::getEntityCutout;
                final VertexConsumer buffer = consumers.getBuffer(layerFactory.apply(textureData.id()));
                final int lightEmission = MathHelper.clamp((int) Math.round(state.getProperty(ModelRenderState.LIGHT_EMISSION).get() * 15.0), 0, 15);
                drawTextured(buffer, extents, pMat, nMat, color, textureData, context, lightEmission);
            }
            matrices.pop();
            lightMatrices.pop();
        }
    }

    protected void drawUntextured(final VertexConsumer buffer, final Vec3d extents, final Matrix4f pMat, final int color) {
        final float x1 = (float) (extents.x / 2);
        final float y1 = (float) (extents.y / 2);
        final float z1 = (float) (extents.z / 2);
        final float x0 = -x1;
        final float y0 = -y1;
        final float z0 = -z1;

        //DOWN
        buffer.vertex(pMat, x1, y0, z1).color(0xFFFF0000).next();
        buffer.vertex(pMat, x0, y0, z1).color(0xFFFF0000).next();
        buffer.vertex(pMat, x0, y0, z0).color(0xFFFF0000).next();
        buffer.vertex(pMat, x1, y0, z0).color(0xFFFF0000).next();

        //UP
        buffer.vertex(pMat, x1, y1, z0).color(0xFF00FF00).next();
        buffer.vertex(pMat, x0, y1, z0).color(0xFF00FF00).next();
        buffer.vertex(pMat, x0, y1, z1).color(0xFF00FF00).next();
        buffer.vertex(pMat, x1, y1, z1).color(0xFF00FF00).next();

        //WEST
        buffer.vertex(pMat, x0, y0, z0).color(0xFF0000FF).next();
        buffer.vertex(pMat, x0, y0, z1).color(0xFF0000FF).next();
        buffer.vertex(pMat, x0, y1, z1).color(0xFF0000FF).next();
        buffer.vertex(pMat, x0, y1, z0).color(0xFF0000FF).next();

        //EAST
        buffer.vertex(pMat, x1, y0, z1).color(color).next();
        buffer.vertex(pMat, x1, y0, z0).color(color).next();
        buffer.vertex(pMat, x1, y1, z0).color(color).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).next();

        //NORTH
        buffer.vertex(pMat, x1, y0, z0).color(color).next();
        buffer.vertex(pMat, x0, y0, z0).color(color).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).next();
        buffer.vertex(pMat, x1, y1, z0).color(color).next();

        //SOUTH
        buffer.vertex(pMat, x0, y0, z1).color(color).next();
        buffer.vertex(pMat, x1, y0, z1).color(color).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).next();
    }

    protected void drawTextured(final VertexConsumer buffer, final Vec3d extents, final Matrix4f pMat, final Matrix3f nMat, final int color, final ModelRenderState.TextureData data, final BattleRenderContext context, final int emissionLight) {
        final float x1 = (float) (extents.x / 2);
        final float y1 = (float) (extents.y / 2);
        final float z1 = (float) (extents.z / 2);
        final float x0 = -x1;
        final float y0 = -y1;
        final float z0 = -z1;

        final float textureWidth = data.textureWidth();
        final float textureHeight = data.textureHeight();
        final float u = data.u();
        final float v = data.v();
        final float sizeX = data.width();
        final float sizeY = data.height();
        final float sizeZ = data.depth();
        final float u0 = (u) / textureWidth;
        final float u1 = (u + sizeZ) / textureWidth;
        final float u2 = (u + sizeZ + sizeX) / textureWidth;
        final float u3 = (u + sizeZ + sizeX + sizeX) / textureWidth;
        final float u4 = (u + sizeZ + sizeX + sizeZ) / textureWidth;
        final float u5 = (u + sizeZ + sizeX + sizeZ + sizeX) / textureWidth;
        final float v0 = (v) / textureHeight;
        final float v1 = (v + sizeZ) / textureHeight;
        final float v2 = (v + sizeZ + sizeY) / textureHeight;

        final Vector4f vertex0 = new Vector4f(x0, y0, z0, 1);
        pMat.transform(vertex0);

        final Vector4f vertex1 = new Vector4f(x1, y0, z0, 1);
        pMat.transform(vertex1);

        final Vector4f vertex2 = new Vector4f(x1, y1, z0, 1);
        pMat.transform(vertex2);

        final Vector4f vertex3 = new Vector4f(x0, y1, z0, 1);
        pMat.transform(vertex3);

        final Vector4f vertex4 = new Vector4f(x0, y0, z1, 1);
        pMat.transform(vertex4);

        final Vector4f vertex5 = new Vector4f(x1, y0, z1, 1);
        pMat.transform(vertex5);

        final Vector4f vertex6 = new Vector4f(x1, y1, z1, 1);
        pMat.transform(vertex6);

        final Vector4f vertex7 = new Vector4f(x0, y1, z1, 1);
        pMat.transform(vertex7);

        final BattleRenderContext.LightResult lightResult = new BattleRenderContext.LightResult();
        context.light(x0, y0, z0, x1, y1, z1, emissionLight, lightResult);
        final int light0 = lightResult.l0;
        final int light1 = lightResult.l1;
        final int light2 = lightResult.l2;
        final int light3 = lightResult.l3;
        final int light4 = lightResult.l4;
        final int light5 = lightResult.l5;
        final int light6 = lightResult.l6;
        final int light7 = lightResult.l7;
        //DOWN 5401
        final Vector3f scratch = new Vector3f(0, -1, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex5.x, vertex5.y, vertex5.z).color(color).texture(u2, v0).overlay(OverlayTexture.DEFAULT_UV).light(light5).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex4.x, vertex4.y, vertex4.z).color(color).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light4).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex0.x, vertex0.y, vertex0.z).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light0).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex1.x, vertex1.y, vertex1.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light1).normal(scratch.x, scratch.y, scratch.z).next();

        //UP 2376
        scratch.set(0, 1, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex2.x, vertex2.y, vertex2.z).color(color).texture(u3, v1).overlay(OverlayTexture.DEFAULT_UV).light(light2).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex3.x, vertex3.y, vertex3.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light3).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex7.x, vertex7.y, vertex7.z).color(color).texture(u2, v0).overlay(OverlayTexture.DEFAULT_UV).light(light7).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex6.x, vertex6.y, vertex6.z).color(color).texture(u3, v0).overlay(OverlayTexture.DEFAULT_UV).light(light6).normal(scratch.x, scratch.y, scratch.z).next();

        //WEST 0473
        scratch.set(-1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex0.x, vertex0.y, vertex0.z).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light0).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex4.x, vertex4.y, vertex4.z).color(color).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light4).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex7.x, vertex7.y, vertex7.z).color(color).texture(u0, v2).overlay(OverlayTexture.DEFAULT_UV).light(light7).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex3.x, vertex3.y, vertex3.z).color(color).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(light3).normal(scratch.x, scratch.y, scratch.z).next();

        //EAST 5126
        scratch.set(1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex5.x, vertex5.y, vertex5.z).color(color).texture(u4, v1).overlay(OverlayTexture.DEFAULT_UV).light(light5).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex1.x, vertex1.y, vertex1.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light1).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex2.x, vertex2.y, vertex2.z).color(color).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light2).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex6.x, vertex6.y, vertex6.z).color(color).texture(u4, v2).overlay(OverlayTexture.DEFAULT_UV).light(light6).normal(scratch.x, scratch.y, scratch.z).next();

        //NORTH 1032
        scratch.set(0, 0, -1);
        scratch.mul(nMat);
        buffer.vertex(vertex1.x, vertex1.y, vertex1.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light1).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex0.x, vertex0.y, vertex0.z).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light0).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex3.x, vertex3.y, vertex3.z).color(color).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(light3).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex2.x, vertex2.y, vertex2.z).color(color).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light2).normal(scratch.x, scratch.y, scratch.z).next();

        //SOUTH 4567
        scratch.set(0, 0, 1);
        scratch.mul(nMat);
        buffer.vertex(vertex4.x, vertex4.y, vertex4.z).color(color).texture(u5, v1).overlay(OverlayTexture.DEFAULT_UV).light(light4).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex5.x, vertex5.y, vertex5.z).color(color).texture(u4, v1).overlay(OverlayTexture.DEFAULT_UV).light(light5).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex6.x, vertex6.y, vertex6.z).color(color).texture(u4, v2).overlay(OverlayTexture.DEFAULT_UV).light(light6).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex7.x, vertex7.y, vertex7.z).color(color).texture(u5, v2).overlay(OverlayTexture.DEFAULT_UV).light(light7).normal(scratch.x, scratch.y, scratch.z).next();
    }

    protected void applyTransformation(final MatrixStack matrices, final MatrixStack lightMatrices, final ModelRenderState state, final boolean inversion) {
        if (!inversion) {
            state.multiply(matrices);
            state.multiply(lightMatrices);
        } else {
            state.multiplyInverted(matrices);
            state.multiplyInverted(lightMatrices);
        }
    }
}
