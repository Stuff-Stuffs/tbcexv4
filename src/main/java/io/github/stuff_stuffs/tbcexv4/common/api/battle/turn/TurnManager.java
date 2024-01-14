package io.github.stuff_stuffs.tbcexv4.common.api.battle.turn;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TurnManager {
    Set<BattleParticipantHandle> currentTurn();

    default Result<Unit, Text> check(final BattleAction action) {
        final Optional<BattleParticipantHandle> source = action.source();
        if (source.isPresent()) {
            return currentTurn().contains(source.get()) ? new Result.Success<>(Unit.INSTANCE) : new Result.Failure<>(Text.of("Not you turn!"));
        } else {
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    void setup(BattleState state, BattleTransactionContext context, BattleTracer.Span<?> trace);

    void onAction(BattleParticipantHandle source, BattleState state, BattleTransactionContext context, BattleTracer.Span<?> trace);
}
