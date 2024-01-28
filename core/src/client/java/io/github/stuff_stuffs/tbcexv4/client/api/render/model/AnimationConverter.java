package io.github.stuff_stuffs.tbcexv4.client.api.render.model;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.PropertyTypes;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import java.util.*;

public class AnimationConverter implements Animation<ModelRenderState> {
    private final net.minecraft.client.render.entity.animation.Animation delegate;
    private final boolean soft;

    public AnimationConverter(final net.minecraft.client.render.entity.animation.Animation delegate, final boolean soft) {
        this.delegate = delegate;
        this.soft = soft;
    }

    @Override
    public Result<List<TimedEvent>, Unit> animate(final double time, final ModelRenderState state, final AnimationContext context) {
        final var folder = Result.<TimedEvent>mutableFold();
        for (final Map.Entry<String, List<Transformation>> entry : delegate.boneAnimations().entrySet()) {
            final List<ModelRenderState> children = state.getChildren(entry.getKey(), time);
            if (children.isEmpty()) {
                if (soft) {
                    continue;
                } else {
                    return Result.failure(Unit.INSTANCE);
                }
            }
            final Sorted sort = sort(entry.getValue());
            if (!sort.translation.keyframes.isEmpty()) {
                final StateModifier<Vec3d> modifier = StateModifier.split(
                        sort.translation.keyframes,
                        value -> new Keyframe((float) value, null, null),
                        Keyframe::timestamp,
                        keyframe -> new Vec3d(keyframe.target()),
                        PropertyTypes.VEC3D.interpolator(),
                        SEARCH_COMPARATOR
                ).stretch(1 / 20.0).delay(time);
                for (final ModelRenderState child : children) {
                    folder.accept(child.getProperty(ModelRenderState.TRANSLATION).reserve(
                                    modifier, time,
                                    time + delegate.lengthInSeconds() * 20,
                                    Easing.CONSTANT_1, context,
                                    Property.ReservationLevel.ACTION
                            )
                    );
                }
            }
            if (!sort.scaling.keyframes.isEmpty()) {
                final StateModifier<Vec3d> modifier = StateModifier.split(
                        sort.scaling.keyframes,
                        value -> new Keyframe((float) value, null, null),
                        Keyframe::timestamp,
                        keyframe -> new Vec3d(keyframe.target()),
                        PropertyTypes.VEC3D.interpolator(),
                        SEARCH_COMPARATOR
                ).stretch(1 / 20.0).delay(time);
                for (final ModelRenderState child : children) {
                    folder.accept(child.getProperty(ModelRenderState.SCALE).reserve(
                                    modifier, time,
                                    time + delegate.lengthInSeconds() * 20,
                                    Easing.CONSTANT_1, context,
                                    Property.ReservationLevel.ACTION
                            )
                    );
                }
            }
            if (!sort.rotation.keyframes.isEmpty()) {
                final StateModifier<Quaternionfc> modifier = StateModifier.split(
                        sort.rotation.keyframes,
                        value -> new Keyframe((float) value, null, null),
                        Keyframe::timestamp,
                        keyframe -> new Vec3d(keyframe.target()),
                        PropertyTypes.VEC3D.interpolator(),
                        SEARCH_COMPARATOR
                ).<Quaternionfc>map(vec -> new Quaternionf().rotateZYX((float) vec.x, (float) vec.y, (float) vec.z)).stretch(1 / 20.0).delay(time);
                for (final ModelRenderState child : children) {
                    folder.accept(child.getProperty(ModelRenderState.ROTATION).reserve(
                                    modifier, time,
                                    time + delegate.lengthInSeconds() * 20,
                                    Easing.CONSTANT_1, context,
                                    Property.ReservationLevel.ACTION
                            )
                    );
                }
            }
        }
        return folder.get();
    }

    private Sorted sort(final List<Transformation> transformations) {
        final List<Keyframe> translation = new ArrayList<>();
        final List<Keyframe> rotation = new ArrayList<>();
        final List<Keyframe> scaling = new ArrayList<>();
        for (final Transformation transformation : transformations) {
            if (transformation.target() == Transformation.Targets.TRANSLATE) {
                translation.addAll(Arrays.asList(transformation.keyframes()));
            } else if (transformation.target() == Transformation.Targets.ROTATE) {
                rotation.addAll(Arrays.asList(transformation.keyframes()));
            } else if (transformation.target() == Transformation.Targets.SCALE) {
                scaling.addAll(Arrays.asList(transformation.keyframes()));
            }
        }
        translation.sort(SEARCH_COMPARATOR);
        rotation.sort(SEARCH_COMPARATOR);
        scaling.sort(SEARCH_COMPARATOR);
        return new Sorted(createEntry(translation), createEntry(rotation), createEntry(scaling));
    }

    private SortedEntry createEntry(final List<Keyframe> keyframes) {
        if (keyframes.isEmpty()) {
            return new SortedEntry(keyframes, -1, -1);
        }
        return new SortedEntry(keyframes, keyframes.get(0).timestamp(), keyframes.get(keyframes.size() - 1).timestamp());
    }

    private record Sorted(SortedEntry translation, SortedEntry rotation, SortedEntry scaling) {
    }

    private record SortedEntry(List<Keyframe> keyframes, double min, double max) {

    }

    private static final Comparator<Keyframe> SEARCH_COMPARATOR = Comparator.comparingDouble(Keyframe::timestamp);
}
