package io.github.stuff_stuffs.tbcexv4.client.api.render.model;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.client.mixin.*;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.client.model.ModelCuboidData;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModelConverter implements Animation<ParticipantRenderState> {
    private final TexturedModelData data;
    private final Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final boolean transparent;

    public ModelConverter(final TexturedModelData data, final Identifier texture, final boolean transparent) {
        this.data = data;
        this.texture = texture;
        this.transparent = transparent;
        final AccessorTexturedModelData modelData = (AccessorTexturedModelData) data;
        textureWidth = ((AccessorTextureDimensions) modelData.getDimensions()).getWidth();
        textureHeight = ((AccessorTextureDimensions) modelData.getDimensions()).getHeight();
    }

    public static String childId(final int index) {
        return "generated_cuboid_" + index;
    }

    @Override
    public Result<List<TimedEvent>, Unit> animate(final double time, final ParticipantRenderState state, final AnimationContext context) {
        final ModelRenderState modelRoot = state.modelRoot();
        final var folder = Result.<TimedEvent>mutableFold();
        create(((AccessorTexturedModelData) data).getData().getRoot(), modelRoot, time, context, folder, true);
        return folder.get();
    }

    private void create(final ModelPartData data, final ModelRenderState state, final double time, final AnimationContext context, final Result.Folder<List<TimedEvent>, TimedEvent, Unit> folder, final boolean root) {
        if (root) {
            folder.accept(state.getProperty(ModelRenderState.POSITION).setDefaultValue(new Vec3d(0, 1.501, 0), time, context));
        }
        int i = 0;
        final ModelTransform rotationData = ((AccessorModelPartData) data).getRotationData();
        for (final ModelCuboidData cuboidData : ((AccessorModelPartData) data).getCuboidData()) {
            final String id = childId(i++);
            folder.accept(state.addChild(id, time, context));
            final Optional<ModelRenderState> opt = state.getChild(id, time);
            if (opt.isEmpty()) {
                folder.accept(Result.failure(Unit.INSTANCE));
                return;
            }
            final ModelRenderState childState = opt.get();
            folder.accept(childState.getProperty(ModelRenderState.LAST_INVERSION).setDefaultValue(true, time, context));
            final AccessorModelCuboidData modelCuboidData = (AccessorModelCuboidData) (Object) cuboidData;
            final Vector3f size = modelCuboidData.getDimensions();
            final float inv = 1 / 16.0F;
            final AccessorDilation extraSize = (AccessorDilation) modelCuboidData.getExtraSize();
            folder.accept(childState.getProperty(ModelRenderState.EXTENTS).setDefaultValue(new Vec3d((size.x + extraSize.getRadiusX() * 2) * inv, (size.y + extraSize.getRadiusY() * 2) * inv, (size.z + extraSize.getRadiusZ() * 2) * inv), time, context));
            final Vector3f pos = modelCuboidData.getOffset();
            folder.accept(childState.getProperty(ModelRenderState.OFFSET).setDefaultValue(new Vec3d((pos.x + size.x * 0.5) * inv, (pos.y + size.y * 0.5) * inv, (pos.z + size.z * 0.5) * inv), time, context));
            folder.accept(childState.getProperty(ModelRenderState.POSITION).setDefaultValue(new Vec3d(rotationData.pivotX * inv, rotationData.pivotY * inv, rotationData.pivotZ * inv), time, context));
            folder.accept(childState.getProperty(ModelRenderState.ROTATION).setDefaultValue(new Quaternionf().rotationZYX(rotationData.roll, rotationData.yaw, rotationData.pitch), time, context));
            final Vector2f uv = modelCuboidData.getTextureUV();
            final Vector2f scale = modelCuboidData.getTextureScale();
            final ModelRenderState.TextureData textureData = new ModelRenderState.TextureData(texture, size.x, size.y, size.z, (int) uv.getX(), (int) uv.getY(), textureWidth * scale.getX(), textureHeight * scale.getY(), transparent);
            folder.accept(childState.getProperty(ModelRenderState.TEXTURE_DATA).setDefaultValue(Optional.of(textureData), time, context));
            folder.accept(childState.getProperty(ModelRenderState.RENDERER).setDefaultValue(ModelRendererRegistry.DEFAULT_RENDERER, time, context));
        }
        for (final Map.Entry<String, ModelPartData> entry : ((AccessorModelPartData) data).getChildren().entrySet()) {
            folder.accept(state.addChild(entry.getKey(), time, context));
            final Optional<ModelRenderState> opt = state.getChild(entry.getKey(), time);
            if (opt.isEmpty()) {
                folder.accept(Result.failure(Unit.INSTANCE));
                return;
            }
            final ModelRenderState renderState = opt.get();
            create(entry.getValue(), renderState, time, context, folder, false);
        }
    }
}
