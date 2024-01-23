package io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

public interface BattleAttachment {
    void init(BattleState state, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    void deinit(BattleState state, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);
}
