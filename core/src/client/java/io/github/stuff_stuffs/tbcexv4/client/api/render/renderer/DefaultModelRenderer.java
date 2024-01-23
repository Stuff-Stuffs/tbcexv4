package io.github.stuff_stuffs.tbcexv4.client.api.render.renderer;

import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleRenderContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.*;

import java.util.Optional;
import java.util.function.Function;

public class DefaultModelRenderer implements ModelRenderer {
    private static final Quaternionf SCRATCH_ROTATION = new Quaternionf();

    @Override
    public void render(final BattleRenderContext context, final ModelRenderState state) {
        final Vec3d extents = state.getProperty(ModelRenderState.EXTENTS).get();
        if (extents.lengthSquared() > 0.001) {
            final int color = state.getProperty(ModelRenderState.COLOR).get();
            final MatrixStack matrices = context.parent().matrixStack();
            matrices.push();
            final MatrixStack lightMatrices = context.lightMatrices();
            walkUp(matrices, lightMatrices, state, true);
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
                drawTextured(buffer, extents, pMat, nMat, color, textureData, context);
            }
            matrices.pop();
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
        buffer.vertex(pMat, x1, y0, z1).color(color).next();
        buffer.vertex(pMat, x0, y0, z1).color(color).next();
        buffer.vertex(pMat, x0, y0, z0).color(color).next();
        buffer.vertex(pMat, x1, y0, z0).color(color).next();

        //UP
        buffer.vertex(pMat, x1, y1, z0).color(color).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).next();

        //WEST
        buffer.vertex(pMat, x0, y0, z0).color(color).next();
        buffer.vertex(pMat, x0, y0, z1).color(color).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).next();

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

    protected void drawTextured(final VertexConsumer buffer, final Vec3d extents, final Matrix4f pMat, final Matrix3f nMat, final int color, final ModelRenderState.TextureData data, final BattleRenderContext context) {
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
        vertex0.mul(1 / vertex0.w);

        final Vector4f vertex1 = new Vector4f(x1, y0, z0, 1);
        pMat.transform(vertex1);
        vertex1.mul(1 / vertex1.w);

        final Vector4f vertex2 = new Vector4f(x1, y1, z0, 1);
        pMat.transform(vertex2);
        vertex2.mul(1 / vertex2.w);

        final Vector4f vertex3 = new Vector4f(x0, y1, z0, 1);
        pMat.transform(vertex3);
        vertex3.mul(1 / vertex3.w);

        final Vector4f vertex4 = new Vector4f(x0, y0, z1, 1);
        pMat.transform(vertex4);
        vertex4.mul(1 / vertex4.w);

        final Vector4f vertex5 = new Vector4f(x1, y0, z1, 1);
        pMat.transform(vertex5);
        vertex5.mul(1 / vertex5.w);

        final Vector4f vertex6 = new Vector4f(x1, y1, z1, 1);
        pMat.transform(vertex6);
        vertex6.mul(1 / vertex6.w);

        final Vector4f vertex7 = new Vector4f(x0, y1, z1, 1);
        pMat.transform(vertex7);
        vertex7.mul(1 / vertex7.w);

        final int light0 = context.light(x0, y0, z0);
        final int light1 = context.light(x1, y0, z0);
        final int light2 = context.light(x1, y1, z0);
        final int light3 = context.light(x0, y1, z0);
        final int light4 = context.light(x0, y0, z1);
        final int light5 = context.light(x1, y0, z1);
        final int light6 = context.light(x1, y1, z1);
        final int light7 = context.light(x0, y1, z1);
        //DOWN 5401
        final Vector3f scratch = new Vector3f(0, -1, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex1.x, vertex1.y, vertex1.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light1).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex0.x, vertex0.y, vertex0.z).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light0).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex4.x, vertex4.y, vertex4.z).color(color).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light4).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex5.x, vertex5.y, vertex5.z).color(color).texture(u2, v0).overlay(OverlayTexture.DEFAULT_UV).light(light5).normal(scratch.x, scratch.y, scratch.z).next();

        //UP 2376
        scratch.set(0, 1, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex6.x, vertex6.y, vertex6.z).color(color).texture(u3, v0).overlay(OverlayTexture.DEFAULT_UV).light(light6).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex7.x, vertex7.y, vertex7.z).color(color).texture(u2, v0).overlay(OverlayTexture.DEFAULT_UV).light(light7).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex3.x, vertex3.y, vertex3.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light3).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex2.x, vertex2.y, vertex2.z).color(color).texture(u3, v1).overlay(OverlayTexture.DEFAULT_UV).light(light2).normal(scratch.x, scratch.y, scratch.z).next();

        //WEST 0473
        scratch.set(-1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex3.x, vertex3.y, vertex3.z).color(color).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(light3).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex7.x, vertex7.y, vertex7.z).color(color).texture(u0, v2).overlay(OverlayTexture.DEFAULT_UV).light(light7).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex4.x, vertex4.y, vertex4.z).color(color).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light4).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex0.x, vertex0.y, vertex0.z).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light0).normal(scratch.x, scratch.y, scratch.z).next();

        //EAST 5126
        scratch.set(1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(vertex6.x, vertex6.y, vertex6.z).color(color).texture(u4, v2).overlay(OverlayTexture.DEFAULT_UV).light(light6).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex2.x, vertex2.y, vertex2.z).color(color).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light2).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex1.x, vertex1.y, vertex1.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light1).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex5.x, vertex5.y, vertex5.z).color(color).texture(u4, v1).overlay(OverlayTexture.DEFAULT_UV).light(light5).normal(scratch.x, scratch.y, scratch.z).next();

        //NORTH 1032
        scratch.set(0, 0, -1);
        scratch.mul(nMat);
        buffer.vertex(vertex2.x, vertex2.y, vertex2.z).color(color).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light2).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex3.x, vertex3.y, vertex3.z).color(color).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(light3).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex0.x, vertex0.y, vertex0.z).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light0).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex1.x, vertex1.y, vertex1.z).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light1).normal(scratch.x, scratch.y, scratch.z).next();

        //SOUTH 4567
        scratch.set(0, 0, 1);
        scratch.mul(nMat);
        buffer.vertex(vertex7.x, vertex7.y, vertex7.z).color(color).texture(u5, v2).overlay(OverlayTexture.DEFAULT_UV).light(light7).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex6.x, vertex6.y, vertex6.z).color(color).texture(u4, v2).overlay(OverlayTexture.DEFAULT_UV).light(light6).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex5.x, vertex5.y, vertex5.z).color(color).texture(u4, v1).overlay(OverlayTexture.DEFAULT_UV).light(light5).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(vertex4.x, vertex4.y, vertex4.z).color(color).texture(u5, v1).overlay(OverlayTexture.DEFAULT_UV).light(light4).normal(scratch.x, scratch.y, scratch.z).next();
    }

    protected void walkUp(final MatrixStack matrices, final MatrixStack lightMatrices, final ModelRenderState state, final boolean root) {
        final RenderState parent = state.parent();
        if (parent instanceof final ParticipantRenderState participant) {
            final Vec3d position = participant.getProperty(ParticipantRenderState.POSITION).get();
            matrices.translate(position.x, position.y, position.z);
            lightMatrices.translate(position.x, position.y, position.z);
        } else if (parent instanceof final ModelRenderState next) {
            walkUp(matrices, lightMatrices, next, false);
        }
        final Vec3d position = state.getProperty(ModelRenderState.POSITION).get();
        matrices.translate(position.x, position.y, position.z);
        lightMatrices.translate(position.x, position.y, position.z);
        final Quaternionfc rotation = state.getProperty(ModelRenderState.ROTATION).get();
        matrices.multiply(rotation.get(SCRATCH_ROTATION));
        lightMatrices.multiply(rotation.get(SCRATCH_ROTATION));
        if (root) {
            final Vec3d offset = state.getProperty(ModelRenderState.OFFSET).get();
            matrices.translate(offset.x, offset.y, offset.z);
            lightMatrices.translate(offset.x, offset.y, offset.z);
        }
    }
}
