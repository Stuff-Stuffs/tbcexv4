package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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
        return Tbcexv4Util.resolveRegistryKey(parent, sourceWorld).resolve(id.toString() + ".battle");
    }
}
