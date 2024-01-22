package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.Supplier;

public record PropertyType<T>(Class<T> clazz, Codec<T> codec, Interpolator<T> interpolator, Supplier<T> defaultValue) {
    private static final Map<Identifier, PropertyType<?>> REGISTRY = new Object2ReferenceOpenHashMap<>();
    private static final Map<PropertyType<?>, Identifier> REVERSE_REGISTRY = new Reference2ObjectOpenHashMap<>();
    public static final Codec<PropertyType<?>> CODEC = Identifier.CODEC.flatXmap(PropertyType::getResult, PropertyType::id);

    public static <T> void register(final Identifier id, final PropertyType<T> propertyType) {
        if (REGISTRY.putIfAbsent(id, propertyType) != null) {
            throw new RuntimeException();
        }
        if (REVERSE_REGISTRY.putIfAbsent(propertyType, id) != null) {
            REGISTRY.remove(id);
            throw new RuntimeException();
        }
    }

    private static DataResult<PropertyType<?>> getResult(final Identifier id) {
        final PropertyType<?> type = REGISTRY.get(id);
        if (type != null) {
            return DataResult.success(type);
        } else {
            return DataResult.error(() -> "Unknown property type: " + id);
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

    private DataResult<Identifier> id() {
        final Identifier identifier = REVERSE_REGISTRY.get(this);
        if (identifier != null) {
            return DataResult.success(identifier);
        } else {
            return DataResult.error(() -> "Unregistered PropertyType with clazz: " + clazz.getName());
        }
    }

    public interface Interpolator<T> {
        T interpolate(T start, T end, double time);
    }
}
