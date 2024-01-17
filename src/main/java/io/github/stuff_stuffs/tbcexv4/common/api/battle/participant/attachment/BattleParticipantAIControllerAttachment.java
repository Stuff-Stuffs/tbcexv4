package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment;

import io.github.stuff_stuffs.tbcexv4.common.api.ai.ActionSearchStrategy;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

import java.util.function.Function;

public final class BattleParticipantAIControllerAttachment implements BattleParticipantAttachment {
    private final Function<BattleParticipant, ActionSearchStrategy> strategyFactory;
    private ActionSearchStrategy strategy;

    public BattleParticipantAIControllerAttachment(final Function<BattleParticipant, ActionSearchStrategy> strategyFactory) {
        this.strategyFactory = strategyFactory;
    }


    public ActionSearchStrategy strategy() {
        return strategy;
    }

    @Override
    public void init(final BattleParticipant participant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        strategy = strategyFactory.apply(participant);
    }

    @Override
    public void deinit(final BattleParticipant participant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {

    }
}
