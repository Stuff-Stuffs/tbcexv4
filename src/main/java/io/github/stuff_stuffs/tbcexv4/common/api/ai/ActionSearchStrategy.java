package io.github.stuff_stuffs.tbcexv4.common.api.ai;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import io.github.stuff_stuffs.tbcexv4.common.impl.ai.BasicActionSearchStrategyImpl;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ActionSearchStrategy {
    Optional<List<BattleAction>> search(TurnManager manager, BattleParticipant participant, Scorer scorer, BattleTracer tracer, BattleTransactionContext context, long seed, CompletableFuture<CompletableFuture<Unit>> cancellation);

    static ActionSearchStrategy basic(final double temperature) {
        if (temperature <= 0) {
            throw new IllegalArgumentException();
        }
        return new BasicActionSearchStrategyImpl(temperature);
    }
}
