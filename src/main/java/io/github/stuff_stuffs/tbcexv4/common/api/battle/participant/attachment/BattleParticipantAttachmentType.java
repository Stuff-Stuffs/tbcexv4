package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;

import java.util.function.Function;

public final class BattleParticipantAttachmentType<T extends BattleParticipantAttachment> {
    private final Function<BattleCodecContext, Codec<T>> codecFactory;

    public BattleParticipantAttachmentType(final Function<BattleCodecContext, Codec<T>> factory) {
        codecFactory = factory;
    }

    public Codec<T> codec(final BattleCodecContext context) {
        return codecFactory.apply(context);
    }
}
