package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

public interface BattleParticipantAttachment {
    void init(BattleParticipant participant, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    void deinit(BattleParticipant participant, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    interface Builder {
        <T extends BattleParticipantAttachment> void accept(T value, BattleParticipantAttachmentType<T> type);
    }
}
