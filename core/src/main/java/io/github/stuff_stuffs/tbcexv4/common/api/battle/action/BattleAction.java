package io.github.stuff_stuffs.tbcexv4.common.api.battle.action;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

import java.util.Optional;

public interface BattleAction {
    BattleActionType<?> type();

    boolean apply(BattleState state, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    default Optional<ActionSource> source() {
        return Optional.empty();
    }

    static Codec<BattleAction> codec(final BattleCodecContext codecContext) {
        return Tbcexv4Registries.BattleActionTypes.CODEC.dispatchStable(BattleAction::type, type -> type.codec(codecContext));
    }
}
