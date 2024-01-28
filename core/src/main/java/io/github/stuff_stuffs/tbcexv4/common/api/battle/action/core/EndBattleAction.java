package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;

public class EndBattleAction implements BattleAction {
    public static final Codec<EndBattleAction> CODEC = Codec.unit(new EndBattleAction());

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActionTypes.END_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> span) {
        ((BattleStateImpl) state).finish(transactionContext, span);
        return true;
    }
}
