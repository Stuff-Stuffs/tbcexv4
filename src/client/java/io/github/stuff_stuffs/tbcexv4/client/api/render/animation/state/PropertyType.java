package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.Identifier;

import java.util.Map;

public record PropertyType<T>(Class<T> clazz, Codec<T> codec, Interpolator<T> interpolator) {
    private static final Map<Identifier, PropertyType<?>> REGISTRY = new Object2ReferenceOpenHashMap<>();

    public static <T> void register(final Identifier id, final PropertyType<T> propertyType) {
        if (REGISTRY.putIfAbsent(id, propertyType) != null) {
            throw new RuntimeException();
        }
    }

    public static PropertyType<?> get(final Identifier id) {
        return REGISTRY.get(id);
    }

    public static <T> PropertyType<T> getTyped(final Identifier id, final Class<T> clazz) {
        final PropertyType<?> type = REGISTRY.get(id);
        if (type.clazz == clazz) {
            return null;
        }
        //noinspection unchecked
        return (PropertyType<T>) type;
    }

    public interface Interpolator<T> {
        T interpolate(T start, T end, double time);
    }
}
