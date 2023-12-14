package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record BattleParticipantPlayerControllerAttachment(UUID controllerId) implements BattleParticipantAttachment {
    public static final Codec<BattleParticipantPlayerControllerAttachment> CODEC = Uuids.STRING_CODEC.xmap(BattleParticipantPlayerControllerAttachment::new, BattleParticipantPlayerControllerAttachment::controllerId);

    @Override
    public BattleParticipantAttachmentType<?> type() {
        return Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLER_TYPE;
    }
}
