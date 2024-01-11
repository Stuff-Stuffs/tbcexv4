package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

import java.util.UUID;

public record BattleParticipantPlayerControllerAttachment(
        UUID controllerId
) implements BattleParticipantAttachment, BattleParticipantPlayerControllerAttachmentView {
    @Override
    public void init(final BattleParticipant participant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {

    }

    @Override
    public void deinit(final BattleParticipant participant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {

    }
}
