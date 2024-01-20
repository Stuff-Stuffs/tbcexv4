package io.github.stuff_stuffs.tbcexv4.client.api.render.renderer;

import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleRenderContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.*;

import java.util.Optional;

public class DefaultModelRenderer implements ModelRenderer {
    private static final Quaternionf SCRATCH_ROTATION = new Quaternionf();

    @Override
    public void render(final BattleRenderContext context, final ModelRenderState state) {
        final Vec3d extents = state.getProperty(ModelRenderState.EXTENTS).get();
        final int color = state.getProperty(ModelRenderState.COLOR).get();
        if (extents.lengthSquared() > 0.001) {
            final Optional<ModelRenderState.TextureData> data = state.getProperty(ModelRenderState.TEXTURE_DATA).get();
            final MatrixStack matrices = context.parent().matrixStack();
            matrices.push();
            walkUp(matrices, state);
            final Matrix4f pMat = matrices.peek().getPositionMatrix();
            final Matrix3f nMat = matrices.peek().getNormalMatrix();
            final VertexConsumerProvider consumers = context.parent().consumers();
            if (data.isEmpty()) {
                final VertexConsumer buffer = consumers.getBuffer(RenderLayer.getDebugQuads());
                drawUntextured(buffer, extents, pMat, color);
            } else {
                final ModelRenderState.TextureData textureData = data.get();
                final VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityTranslucent(textureData.id()));
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

        final int u = data.u();
        final int v = data.v();
        final int sizeX = data.width();
        final int sizeY = data.height();
        final int sizeZ = data.depth();
        final float textureWidth = data.textureWidth();
        final float textureHeight = data.textureHeight();
        final float j = u / textureWidth;
        final float k = (u + sizeZ) / textureWidth;
        final float l = (u + sizeZ + sizeX) / textureWidth;
        final float m = (u + sizeZ + sizeX + sizeX) / textureWidth;
        final float n = (u + sizeZ + sizeX + sizeZ) / textureWidth;
        final float o = (u + sizeZ + sizeX + sizeZ + sizeX) / textureWidth;
        final float p = v / textureHeight;
        final float q = (v + sizeZ) / textureHeight;
        final float r = (v + sizeZ + sizeY) / textureHeight;

        final int light = LightmapTextureManager.pack(15, 15);
        //DOWN
        final Vector3f scratch = new Vector3f(0, -1, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y0, z1).color(color).texture(k, p).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z1).color(color).texture(l, p).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z0).color(color).texture(l, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y0, z0).color(color).texture(k, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //UP
        scratch.set(0, 1, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y1, z0).color(color).texture(l, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).texture(m, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).texture(m, p).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).texture(l, p).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //WEST
        scratch.set(-1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x0, y0, z0).color(color).texture(j, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z1).color(color).texture(k, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).texture(k, r).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).texture(j, r).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //EAST
        scratch.set(1, 0, 0);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y0, z1).color(color).texture(l, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y0, z0).color(color).texture(n, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z0).color(color).texture(n, n).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).texture(l, n).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //NORTH
        scratch.set(0, 0, -1);
        scratch.mul(nMat);
        buffer.vertex(pMat, x1, y0, z0).color(color).texture(k, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y0, z0).color(color).texture(l, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z0).color(color).texture(l, r).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z0).color(color).texture(k, r).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();

        //SOUTH
        scratch.set(0, 0, 1);
        scratch.mul(nMat);
        buffer.vertex(pMat, x0, y0, z1).color(color).texture(n, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y0, z1).color(color).texture(o, q).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x1, y1, z1).color(color).texture(o, r).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
        buffer.vertex(pMat, x0, y1, z1).color(color).texture(n, r).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(scratch.x, scratch.y, scratch.z).next();
    }

    protected void walkUp(final MatrixStack matrices, final ModelRenderState state) {
        final RenderState parent = state.parent();
        if (parent instanceof final ParticipantRenderState participant) {
            final Vec3d position = participant.getProperty(ParticipantRenderState.POSITION).get();
            matrices.translate(position.x, position.y, position.z);
            return;
        } else if (parent instanceof final ModelRenderState next) {
            walkUp(matrices, next);
        }
        final Vec3d position = parent.getProperty(ModelRenderState.POSITION).get();
        matrices.translate(position.x, position.y, position.z);
        final Quaternionfc rotation = parent.getProperty(ModelRenderState.ROTATION).get();
        matrices.multiply(rotation.get(SCRATCH_ROTATION));
    }
}
