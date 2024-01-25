package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.function.Function;

public final class Tbcexv4Util {
    public static final Codec<NbtCompound> NBT_COMPOUND_CODEC = Codecs.fromOps(NbtOps.INSTANCE).comapFlatMap(element -> {
        if (element instanceof NbtCompound compound) {
            return DataResult.success(compound);
        }
        return DataResult.error(() -> "Expected root nbt to be NbtCompound, type 10, got type " + element.getType());
    }, Function.identity());
    public static final Codec<Quaternionfc> ROTATION_CODEC = Codecs.xor(RecordCodecBuilder.<Vector3f>create(instance -> instance.group(
            Codec.FLOAT.fieldOf("yaw").forGetter(Vector3f::x),
            Codec.FLOAT.fieldOf("pitch").forGetter(Vector3f::y),
            Codec.FLOAT.fieldOf("roll").forGetter(Vector3f::z)
    ).apply(instance, Vector3f::new)), RecordCodecBuilder.<Quaternionfc>create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(Quaternionfc::x),
            Codec.FLOAT.fieldOf("y").forGetter(Quaternionfc::y),
            Codec.FLOAT.fieldOf("z").forGetter(Quaternionfc::z),
            Codec.FLOAT.fieldOf("w").forGetter(Quaternionfc::w)
    ).apply(instance, Quaternionf::new))).xmap(either -> either.map(vec -> new Quaternionf().rotateZYX(vec.z, vec.y, vec.x), Function.identity()), quat -> {
        Quaternionf quaternion = quat.get(new Quaternionf());
        float length2 = Math.abs(quaternion.dot(quaternion));
        if(0.999 < length2 && length2 < 1.001) {
            return Either.left(quaternion.getEulerAnglesZYX(new Vector3f()));
        }else  {
            return Either.right(quat);
        }
    });

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
