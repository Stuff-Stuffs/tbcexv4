package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record BattleParticipantHandle(UUID id) {
    public static final Codec<BattleParticipantHandle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.STRING_CODEC.fieldOf("id").forGetter(BattleParticipantHandle::id)
    ).apply(
            instance,
            BattleParticipantHandle::new
    ));
}
