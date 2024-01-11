package io.github.stuff_stuffs.tbcexv4.common.api.ai;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.impl.ai.BasicActionSearchStrategyImpl;

import java.util.Optional;

public interface ActionSearchStrategy {
    Optional<BattleActionRequest> search(BattleParticipant participant, Scorer scorer, BattleTracer tracer, BattleTransactionContext context, long seed);

    static ActionSearchStrategy basic(final double temperature) {
        if (temperature <= 0) {
            throw new IllegalArgumentException();
        }
        return new BasicActionSearchStrategyImpl(temperature);
    }
}
