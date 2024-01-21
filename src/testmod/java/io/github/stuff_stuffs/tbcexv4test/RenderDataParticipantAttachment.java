package io.github.stuff_stuffs.tbcexv4test;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

public class RenderDataParticipantAttachment implements BattleParticipantAttachment, RenderDataParticipantAttachmentView {
    private final Type type;

    public RenderDataParticipantAttachment(final Type type) {
        this.type = type;
    }

    @Override
    public void init(final BattleParticipant participant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {

    }

    @Override
    public void deinit(final BattleParticipant participant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {

    }

    @Override
    public Object traceSnapshot() {
        return this;
    }

    @Override
    public Type type() {
        return type;
    }
}
