package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.BattleEffectRenderer;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.BattleEffectRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRenderer;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import java.util.Optional;

public final class PropertyTypes {
    public static final PropertyType<Vec3d> VEC3D = new PropertyType<>(Vec3d.class, Vec3d.CODEC, (start, end, time) -> start.multiply(1 - time).add(end.multiply(time)), () -> Vec3d.ZERO);
    public static final PropertyType<Quaternionfc> ROTATION = new PropertyType<>(Quaternionfc.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(quat -> quat.x()),
            Codec.FLOAT.fieldOf("y").forGetter(quat -> quat.y()),
            Codec.FLOAT.fieldOf("z").forGetter(quat -> quat.z()),
            Codec.FLOAT.fieldOf("w").forGetter(quat -> quat.w())
    ).apply(instance, Quaternionf::new)), (start, end, time) -> start.slerp(end, (float) time, new Quaternionf()), () -> new Quaternionf(0, 0, 0, 1));
    public static final PropertyType<Unit> LOCK = new PropertyType<>(Unit.class, Codec.unit(Unit.INSTANCE), (start, end, time) -> start, () -> Unit.INSTANCE);
    public static final PropertyType<ModelRenderer> MODEL_RENDERER = new PropertyType<>(ModelRenderer.class, ModelRendererRegistry.CODEC, (start, end, time) -> end, () -> ModelRendererRegistry.NOOP_RENDERER);
    public static final PropertyType<Integer> COLOR = new PropertyType<>(Integer.class, Codec.INT, (start, end, time) -> {
        final double a0 = (start >> 24) & 0xFF;
        final double r0 = (start >> 16) & 0xFF;
        final double g0 = (start >> 8) & 0xFF;
        final double b0 = (start >> 0) & 0xFF;

        final double a1 = (end >> 24) & 0xFF;
        final double r1 = (end >> 16) & 0xFF;
        final double g1 = (end >> 8) & 0xFF;
        final double b1 = (end >> 0) & 0xFF;

        final int a = MathHelper.clamp((int) Math.round(a0 * (1 - time) + a1 * time), 0, 255);
        final int r = MathHelper.clamp((int) Math.round(r0 * (1 - time) + r1 * time), 0, 255);
        final int g = MathHelper.clamp((int) Math.round(g0 * (1 - time) + g1 * time), 0, 255);
        final int b = MathHelper.clamp((int) Math.round(b0 * (1 - time) + b1 * time), 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }, () -> 0xFFFFFFFF);
    public static final PropertyType<Optional<ModelRenderState.TextureData>> TEXTURE_DATA = new PropertyType<>(
            (Class<Optional<ModelRenderState.TextureData>>) (Object) Optional.class,
            ModelRenderState.TextureData.CODEC.optionalFieldOf("texture_data").codec(),
            (start, end, time) -> time < 0.5 ? start : end,
            Optional::empty
    );
    public static final PropertyType<BattleEffectRenderer> BATTLE_EFFECT_RENDERER = new PropertyType<>(BattleEffectRenderer.class, BattleEffectRendererRegistry.CODEC, (start, end, time) -> time < 0.5 ? start : end, () -> BattleEffectRendererRegistry.NOOP_RENDERER);
    public static final PropertyType<Boolean> FLAG = new PropertyType<>(Boolean.class, Codec.BOOL, ((start, end, time) -> time < 0.5 ? start : end), () -> false);

    public static void init() {
        PropertyType.register(Tbcexv4.id("vec3d"), VEC3D);
        PropertyType.register(Tbcexv4.id("rotation"), ROTATION);
        PropertyType.register(Tbcexv4.id("lock"), LOCK);
        PropertyType.register(Tbcexv4.id("model_renderer"), MODEL_RENDERER);
        PropertyType.register(Tbcexv4.id("color"), COLOR);
        PropertyType.register(Tbcexv4.id("texture_data"), TEXTURE_DATA);
        PropertyType.register(Tbcexv4.id("battle_effect_renderer"), BATTLE_EFFECT_RENDERER);
        PropertyType.register(Tbcexv4.id("flag"), FLAG);
    }

    private PropertyTypes() {
    }
}
