package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.minecraft.util.math.Vec3d;

public final class PropertyTypes {
    public static final PropertyType<Vec3d> VEC3D = new PropertyType<>(Vec3d.class, Vec3d.CODEC, (start, end, time) -> start.multiply(1 - time).add(end.multiply(time)));
    public static final PropertyType<Unit> LOCK = new PropertyType<>(Unit.class, Codec.unit(Unit.INSTANCE), (start, end, time) -> start);

    public static void init() {
        PropertyType.register(Tbcexv4.id("vec3d"), VEC3D);
        PropertyType.register(Tbcexv4.id("lock"), LOCK);
    }

    private PropertyTypes() {
    }
}
