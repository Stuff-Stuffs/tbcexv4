package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;

public interface BattleParticipantAttachment {
    BattleParticipantAttachmentType<?> type();

    interface Builder {
        <T extends BattleParticipantAttachment> void accept(T value, BattleParticipantAttachmentType<T> type);
    }

    static Codec<BattleParticipantAttachment> codec(final BattleCodecContext context) {
        return Tbcexv4Registries.BattleParticipantAttachmentTypes.CODEC.dispatchStable(BattleParticipantAttachment::type, type -> type.codec(context));
    }
}
