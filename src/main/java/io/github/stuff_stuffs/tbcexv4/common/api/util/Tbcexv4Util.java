package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.dynamic.Codecs;

import java.util.function.Function;

public final class Tbcexv4Util {
    public static final Codec<NbtCompound> NBT_COMPOUND_CODEC = Codecs.fromOps(NbtOps.INSTANCE).comapFlatMap(element -> {
        if (element instanceof NbtCompound compound) {
            return DataResult.success(compound);
        }
        return DataResult.error(() -> "Expected root nbt to be NbtCompound, type 10, got type " + element.getType());
    }, Function.identity());

    public static <T> T selectFirst(final T first, final T second) {
        return first;
    }

    public static <T> T selectSecond(final T first, final T second) {
        return second;
    }

    public static <S, T extends S> Codec<S> implCodec(final Codec<T> codec, final Class<T> clazz) {
        return codec.flatComapMap(val -> val, val -> {
            if (clazz.isInstance(val)) {
                return DataResult.success((T) val);
            }
            return DataResult.error(() -> "Somebody implemented internal interface " + clazz + " with class " + val.getClass());
        });
    }

    private Tbcexv4Util() {
    }
}
