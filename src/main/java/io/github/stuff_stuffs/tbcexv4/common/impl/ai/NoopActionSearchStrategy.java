package io.github.stuff_stuffs.tbcexv4.common.impl.ai;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.ActionSearchStrategy;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.Scorer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class NoopActionSearchStrategy implements ActionSearchStrategy {
    @Override
    public Optional<List<BattleAction>> search(final TurnManager manager, final BattleParticipant participant, final Scorer scorer, final BattleTracer tracer, final BattleTransactionContext context, final long seed, final CompletableFuture<CompletableFuture<Unit>> cancellation) {
        return Optional.empty();
    }
}
