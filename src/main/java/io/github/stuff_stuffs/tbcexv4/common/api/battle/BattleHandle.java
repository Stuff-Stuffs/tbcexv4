package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.UUID;

public record BattleHandle(RegistryKey<World> sourceWorld, UUID id) {
    public static final Codec<BattleHandle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("source").forGetter(BattleHandle::sourceWorld),
            Uuids.STRING_CODEC.fieldOf("id").forGetter(BattleHandle::id)
    ).apply(instance, BattleHandle::new));

    public Path toPath(final Path parent) {
        final Identifier value = sourceWorld.getValue();
        Path path = parent.resolve(value.getNamespace());
        final String idPath = value.getPath();
        for (final String s : idPath.split("/")) {
            path = path.resolve(s);
        }
        return path.resolve(id.toString().replace('-', '_') + ".battle");
    }
}
