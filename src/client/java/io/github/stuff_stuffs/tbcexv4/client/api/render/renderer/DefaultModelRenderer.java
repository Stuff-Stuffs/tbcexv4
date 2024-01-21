package io.github.stuff_stuffs.tbcexv4.client.api.render.renderer;

import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleRenderContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import net.minecraft.client.render.*;
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
            walkUp(matrices, state, true);
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
                drawTextured(buffer, extents, pMat, nMat, color, textureData);
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

    protected void drawTextured(final VertexConsumer buffer, final Vec3d extents, final Matrix4f pMat, final Matrix3f nMat, final int color, final ModelRenderState.TextureData data) {
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

        final int light = LightmapTextureManager.pack(15, 15);
        //DOWN
        final Vector3f scratch = new Vector3f(0, -1, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y0, z1).color(color).texture(u2, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z1).color(color).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z0).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y0, z0).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //UP
        scratch.set(0, 1, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y1, z0).color(color).texture(u3, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).texture(u2, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).texture(u3, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //WEST
        scratch.set(-1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x0, y0, z0).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z1).color(color).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).texture(u0, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //EAST
        scratch.set(1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y0, z1).color(color).texture(u4, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y0, z0).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z0).color(color).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).texture(u4, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //NORTH
        scratch.set(0, 0, -1);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y0, z0).color(color).texture(u2, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z0).color(color).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).texture(u1, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z0).color(color).texture(u2, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //SOUTH
        scratch.set(0, 0, 1);
        scratch.mul(nMat);
        buffer.vertex(pMat, x0, y0, z1).color(color).texture(u5, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y0, z1).color(color).texture(u4, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).texture(u4, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).texture(u5, v2).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
    }

    protected void walkUp(final MatrixStack matrices, final ModelRenderState state, final boolean root) {
        final RenderState parent = state.parent();
        if (parent instanceof final ParticipantRenderState participant) {
            final Vec3d position = participant.getProperty(ParticipantRenderState.POSITION).get();
            matrices.translate(position.x, position.y, position.z);
        } else if (parent instanceof final ModelRenderState next) {
            walkUp(matrices, next, false);
        }
        final Vec3d position = state.getProperty(ModelRenderState.POSITION).get();
        matrices.translate(position.x, position.y, position.z);
        final Quaternionfc rotation = state.getProperty(ModelRenderState.ROTATION).get();
        matrices.multiply(rotation.get(SCRATCH_ROTATION));
        if (root) {
            final Vec3d offset = state.getProperty(ModelRenderState.OFFSET).get();
            matrices.translate(offset.x, offset.y, offset.z);
        }
    }
}
