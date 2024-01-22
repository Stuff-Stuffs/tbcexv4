package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyKey;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyTypes;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRenderer;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionfc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ModelRenderState extends RenderState {
    PropertyKey<Vec3d> OFFSET = new PropertyKey<>("offset", PropertyTypes.VEC3D);
    PropertyKey<Vec3d> POSITION = new PropertyKey<>("position", PropertyTypes.VEC3D);
    PropertyKey<Vec3d> EXTENTS = new PropertyKey<>("extents", PropertyTypes.VEC3D);
    PropertyKey<Integer> COLOR = new PropertyKey<>("color", PropertyTypes.COLOR);
    PropertyKey<Quaternionfc> ROTATION = new PropertyKey<>("rotation", PropertyTypes.ROTATION);
    PropertyKey<ModelRenderer> RENDERER = new PropertyKey<>("model_renderer", PropertyTypes.MODEL_RENDERER);
    PropertyKey<Optional<TextureData>> TEXTURE_DATA = new PropertyKey<>("texture_data", PropertyTypes.TEXTURE_DATA);

    Optional<ModelRenderState> getChild(String id, double time);

    Result<Animation.TimedEvent, Unit> addChild(String id, double time, AnimationContext context);

    Result<Animation.TimedEvent, Unit> removeChild(String id, double time, AnimationContext context);

    Set<String> children(double time);

    @Override
    RenderState parent();

    static Animation<ModelRenderState> liftOutOf(final Animation<ModelRenderState> animation, final String id) {
        return (time, state, context) -> {
            final Optional<ModelRenderState> child = state.getChild(id, time);
            if (child.isEmpty()) {
                return Result.failure(Unit.INSTANCE);
            }
            return animation.animate(time, child.get(), context);
        };
    }

    static Animation<ParticipantRenderState> lift(final Animation<ModelRenderState> animation, final List<String> path) {
        return (time, state, context) -> {
            ModelRenderState cursor = state.modelRoot();
            for (final String s : path) {
                cursor = cursor.getChild(s, time).orElse(null);
                if (cursor == null) {
                    return Result.failure(Unit.INSTANCE);
                }
            }
            return animation.animate(time, cursor, context);
        };
    }

    static Animation<ParticipantRenderState> lift(final Animation<ModelRenderState> animation, final ModelLiftingPredicate predicate, final boolean soft) {
        return new Animation<>() {
            @Override
            public Result<List<TimedEvent>, Unit> animate(final double time, final ParticipantRenderState state, final AnimationContext context) {
                final List<String> path = new ArrayList<>();
                final ModelRenderState root = state.modelRoot();
                final VisitPathResult result = predicate.visit(root, path, context);
                Result<List<TimedEvent>, Unit> res = result.descend ? visit(root, time, context, path) : Result.success(List.of());
                if (res instanceof final Result.Success<List<TimedEvent>, Unit> success) {
                    if (result.accept) {
                        final Result<List<TimedEvent>, Unit> setup = animation.animate(time, root, context);
                        if (setup instanceof final Result.Success<List<TimedEvent>, Unit> s0) {
                            final List<TimedEvent> combined = new ArrayList<>(success.val().size() + s0.val().size());
                            combined.addAll(success.val());
                            combined.addAll(s0.val());
                            res = Result.success(combined);
                        } else if (!soft) {
                            res = Result.failure(Unit.INSTANCE);
                        }
                    }
                } else if (soft) {
                    final Result<List<TimedEvent>, Unit> setup = animation.animate(time, root, context);
                    res = setup.flatmap(Result::success, unit -> Result.success(List.of()));
                }
                return res;
            }

            private Result<List<TimedEvent>, Unit> visit(final ModelRenderState state, final double time, final AnimationContext context, final List<String> path) {
                final List<TimedEvent> result = new ArrayList<>();
                for (final String childId : state.children(time)) {
                    path.add(childId);
                    final Optional<ModelRenderState> opt = state.getChild(childId, time);
                    if (opt.isEmpty()) {
                        throw new RuntimeException();
                    }
                    final ModelRenderState child = opt.get();
                    final VisitPathResult visitResult = predicate.visit(child, path, context);
                    if (visitResult.descend) {
                        final Result<List<TimedEvent>, Unit> res = visit(child, time, context, path);
                        if (res instanceof final Result.Success<List<TimedEvent>, Unit> success) {
                            result.addAll(success.val());
                        } else if (!soft) {
                            path.remove(path.size() - 1);
                            return res;
                        }
                    }
                    path.remove(path.size() - 1);
                    if (visitResult.accept) {
                        final Result<List<TimedEvent>, Unit> setup = animation.animate(time, child, context);
                        if (setup instanceof final Result.Success<List<TimedEvent>, Unit> success) {
                            result.addAll(success.val());
                        } else if (!soft) {
                            return setup;
                        }
                    }
                }
                return Result.success(result);
            }
        };
    }

    interface ModelLiftingPredicate {
        VisitPathResult visit(ModelRenderState state, List<String> path, AnimationContext context);
    }

    enum VisitPathResult {
        DESCEND(true, false),
        DESCEND_ACCEPT(true, true),
        ACCEPT(false, true),
        SKIP(false, false);
        public final boolean descend;
        public final boolean accept;

        VisitPathResult(final boolean descend, final boolean accept) {
            this.descend = descend;
            this.accept = accept;
        }
    }

    record TextureData(
            Identifier id,
            float width,
            float height,
            float depth,
            float u,
            float v,
            float textureWidth,
            float textureHeight,
            boolean transparent
    ) {
        public static final Codec<TextureData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("id").forGetter(TextureData::id),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("width").forGetter(TextureData::width),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("height").forGetter(TextureData::height),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("depth").forGetter(TextureData::depth),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("u").forGetter(TextureData::u),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("v").forGetter(TextureData::v),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("textureWidth").forGetter(TextureData::u),
                Codec.floatRange(0, Float.MAX_VALUE).fieldOf("textureHeight").forGetter(TextureData::v),
                Codec.BOOL.fieldOf("transparent").forGetter(TextureData::transparent)
        ).apply(instance, TextureData::new));
    }
}
