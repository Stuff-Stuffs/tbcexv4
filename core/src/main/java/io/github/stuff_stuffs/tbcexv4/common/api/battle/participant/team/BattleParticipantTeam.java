package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record BattleParticipantTeam(UUID id) {
    public static final Codec<BattleParticipantTeam> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.STRING_CODEC.fieldOf("id").forGetter(BattleParticipantTeam::id)
    ).apply(instance, BattleParticipantTeam::new));
}
