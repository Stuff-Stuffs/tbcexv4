package io.github.stuff_stuffs.tbcexv4.client.api.render.model;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorDilation;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorModelCuboidData;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorModelPartData;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorTexturedModelData;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import net.minecraft.client.model.ModelCuboidData;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ModelPartConverter implements Animation<ModelRenderState> {
    private final ModelPartData data;
    private final @Nullable Identifier texture;
    private final int textureWidth;
    private final int textureHeight;
    private final boolean transparent;
    private final Map<String, ModelPartConverter> children;

    public ModelPartConverter(final TexturedModelData data) {
        this(((AccessorTexturedModelData) data).getData().getRoot());
    }

    public ModelPartConverter(final ModelPartData data) {
        this.data = data;
        texture = null;
        transparent = false;
        textureWidth = -1;
        textureHeight = -1;
        children = new Object2ReferenceLinkedOpenHashMap<>();
        fillChildren();
    }

    public ModelPartConverter(final ModelPartData data, final Identifier texture, final boolean transparent, final int textureWidth, final int textureHeight) {
        this.data = data;
        this.texture = texture;
        this.transparent = transparent;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        children = new Object2ReferenceLinkedOpenHashMap<>();
        fillChildren();
    }

    private void fillChildren() {
        for (final Map.Entry<String, ModelPartData> entry : ((AccessorModelPartData) data).getChildren().entrySet()) {
            children.put(entry.getKey(), new ModelPartConverter(entry.getValue(), texture, transparent, textureWidth, textureHeight));
        }
    }

    public static String childId(final int index) {
        return "generated_cuboid_" + index;
    }

    @Override
    public Result<List<TimedEvent>, Unit> animate(final double time, final ModelRenderState state, final AnimationContext context) {
        final var folder = Result.<TimedEvent>mutableFold();
        create(state, time, context, folder);
        return folder.get();
    }

    private void create(final ModelRenderState state, final double time, final AnimationContext context, final Result.Folder<List<TimedEvent>, TimedEvent, Unit> folder) {
        final float inv = 1 / 16.0F;
        final ModelTransform rotationData = ((AccessorModelPartData) data).getRotationData();
        folder.accept(state.getProperty(ModelRenderState.MODEL_DATA).setDefaultValue(Optional.of(new ModelRenderState.ModelData(Vec3d.ZERO, Vec3d.ZERO, new Vec3d(rotationData.pivotX * inv, rotationData.pivotY * inv, rotationData.pivotZ * inv), new Quaternionf().rotationZYX(rotationData.roll, rotationData.yaw, rotationData.pitch))), time, context));
        int i = 0;
        for (final ModelCuboidData cuboidData : ((AccessorModelPartData) data).getCuboidData()) {
            final AccessorModelCuboidData modelCuboidData = (AccessorModelCuboidData) (Object) cuboidData;
            final String id = modelCuboidData.getName() != null ? modelCuboidData.getName() : childId(i++);
            folder.accept(state.addChild(id, time, context));
            final Optional<ModelRenderState> opt = state.getChild(id, time);
            if (opt.isEmpty()) {
                folder.accept(Result.failure(Unit.INSTANCE));
                return;
            }
            final ModelRenderState childState = opt.get();
            folder.accept(childState.getProperty(ModelRenderState.LAST_INVERSION).setDefaultValue(true, time, context));
            final Vector3f size = modelCuboidData.getDimensions();
            final ModelRenderState.ModelData modelData = computeModelData(modelCuboidData, size, inv);
            folder.accept(childState.getProperty(ModelRenderState.MODEL_DATA).setDefaultValue(Optional.of(modelData), time, context));
            if (texture != null) {
                final Vector2f uv = modelCuboidData.getTextureUV();
                final Vector2f scale = modelCuboidData.getTextureScale();
                final ModelRenderState.TextureData textureData = new ModelRenderState.TextureData(texture, size.x, size.y, size.z, (int) uv.getX(), (int) uv.getY(), textureWidth * scale.getX(), textureHeight * scale.getY(), transparent);
                folder.accept(childState.getProperty(ModelRenderState.TEXTURE_DATA).setDefaultValue(Optional.of(textureData), time, context));
                folder.accept(childState.getProperty(ModelRenderState.RENDERER).setDefaultValue(ModelRendererRegistry.DEFAULT_RENDERER, time, context));
            }
        }
        for (final Map.Entry<String, ModelPartConverter> entry : children.entrySet()) {
            folder.accept(state.addChild(entry.getKey(), time, context));
            final Optional<ModelRenderState> opt = state.getChild(entry.getKey(), time);
            if (opt.isEmpty()) {
                folder.accept(Result.failure(Unit.INSTANCE));
                return;
            }
            final ModelRenderState renderState = opt.get();
            entry.getValue().create(renderState, time, context, folder);
        }
    }

    private static ModelRenderState.ModelData computeModelData(final AccessorModelCuboidData modelCuboidData, final Vector3f size, final float inv) {
        final AccessorDilation extraSize = (AccessorDilation) modelCuboidData.getExtraSize();
        final Vec3d extents = new Vec3d((size.x + extraSize.getRadiusX() * 2) * inv, (size.y + extraSize.getRadiusY() * 2) * inv, (size.z + extraSize.getRadiusZ() * 2) * inv);
        final Vector3f pos = modelCuboidData.getOffset();
        final Vec3d offset = new Vec3d((pos.x + size.x * 0.5) * inv, (pos.y + size.y * 0.5) * inv, (pos.z + size.z * 0.5) * inv);
        return new ModelRenderState.ModelData(extents, offset, Vec3d.ZERO, new Quaternionf(0, 0, 0, 1));
    }
}
