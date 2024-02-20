package io.github.stuff_stuffs.tbcexv4.client.api.render.model;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorTextureDimensions;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorTexturedModelData;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.TextureDimensions;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ModelConverter implements Animation<ParticipantRenderState> {
    private final ModelPartConverter child;

    public ModelConverter(final TexturedModelData data) {
        this(((AccessorTexturedModelData) data).getData().getRoot());
    }

    public ModelConverter(final ModelPartData data) {
        child = new ModelPartConverter(data);
    }

    public ModelConverter(final TexturedModelData data, final Identifier texture, final boolean transparent) {
        final TextureDimensions dimensions = ((AccessorTexturedModelData) data).getDimensions();
        child = new ModelPartConverter(((AccessorTexturedModelData) data).getData().getRoot(), texture, transparent, ((AccessorTextureDimensions) dimensions).getWidth(), ((AccessorTextureDimensions) dimensions).getHeight());
    }

    @Override
    public Result<List<TimedEvent>, Unit> animate(final double time, final ParticipantRenderState state, final AnimationContext context) {
        final ModelRenderState modelRoot = state.modelRoot();
        final Result<List<TimedEvent>, Unit> result = child.animate(time, modelRoot, context);
        if (result instanceof final Result.Success<List<TimedEvent>, Unit> success) {
            final var folder = Result.<TimedEvent>mutableFold();
            for (final TimedEvent event : success.val()) {
                folder.acceptRaw(event);
            }
            folder.accept(modelRoot.getProperty(ModelRenderState.TRANSLATION).setDefaultValue(new Vec3d(0, -1.501, 0), time, context));
            return folder.get();
        }
        return result;
    }
}
