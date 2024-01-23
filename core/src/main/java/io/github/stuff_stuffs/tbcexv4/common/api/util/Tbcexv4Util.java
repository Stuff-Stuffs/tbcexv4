package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.nio.file.Path;
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

    public static Text concat(final Text... texts) {
        final MutableText text = MutableText.of(PlainTextContent.EMPTY);
        for (final Text t : texts) {
            text.append(t);
        }
        return text;
    }

    public static <Api, Impl extends Api> Codec<Api> implCodec(final Codec<Impl> codec, final Class<Impl> clazz) {
        return codec.flatComapMap(val -> val, val -> {
            if (clazz.isInstance(val)) {
                return DataResult.success((Impl) val);
            }
            return DataResult.error(() -> "Somebody implemented internal interface " + clazz + " with class " + val.getClass());
        });
    }

    public static Path resolveRegistryKey(final Path parent, final RegistryKey<?> key) {
        final Identifier value = key.getValue();
        Path path = parent.resolve(value.getNamespace());
        final String idPath = value.getPath();
        for (final String s : idPath.split("/")) {
            path = path.resolve(s);
        }
        return path;
    }

    private Tbcexv4Util() {
    }
}
