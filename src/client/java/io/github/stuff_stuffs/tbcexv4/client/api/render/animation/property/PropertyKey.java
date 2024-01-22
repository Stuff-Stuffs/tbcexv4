package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PropertyKey<T>(String id, PropertyType<T> type) {
    public static final Codec<PropertyKey<?>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(PropertyKey::id),
            PropertyType.CODEC.fieldOf("type").forGetter(PropertyKey::type)
    ).apply(instance, PropertyKey::new));
}
