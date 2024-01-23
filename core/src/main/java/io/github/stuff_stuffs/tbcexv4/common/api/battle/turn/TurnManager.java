package io.github.stuff_stuffs.tbcexv4.common.api.battle.turn;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.ActionSource;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TurnManager {
    Set<BattleParticipantHandle> currentTurn();

    default Result<Unit, Text> check(final BattleAction action) {
        final Optional<BattleParticipantHandle> source = action.source().map(ActionSource::actor);
        if (source.isPresent()) {
            return currentTurn().contains(source.get()) ? new Result.Success<>(Unit.INSTANCE) : new Result.Failure<>(Text.of("Not you turn!"));
        } else {
            return new Result.Success<>(Unit.INSTANCE);
        }
    }

    boolean checkAi(List<BattleAction> actions);

    BattleAction skipTurn(BattleParticipantHandle handle);

    void setup(BattleState state, BattleTransactionContext context, BattleTracer.Span<?> trace);

    void onAction(int energy, BattleParticipantHandle source, BattleState state, BattleTransactionContext context, BattleTracer.Span<?> trace);
}
