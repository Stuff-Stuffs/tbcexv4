package io.github.stuff_stuffs.tbcexv4.common.api.battle.turn;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

import java.util.Set;

public interface TurnManager {
    Set<BattleParticipantHandle> currentTurn();

    void setup(BattleState state, BattleTransactionContext context, BattleTracer.Span<?> trace);

    void onAction(BattleParticipantHandle source, BattleState state, BattleTransactionContext context, BattleTracer.Span<?> trace);
}
