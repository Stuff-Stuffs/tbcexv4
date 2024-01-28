package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;

import java.util.function.Function;

public class StartBattleAction implements BattleAction {
    public static final Function<BattleCodecContext, Codec<StartBattleAction>> CODEC_FACTORY = context -> Codec.unit(StartBattleAction::new);

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActionTypes.START_BATTLE_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        try (final var transaction = transactionContext.openNested()) {
            ((BattleStateImpl) state).start(transaction);
            transaction.commit();
        }
        return true;
    }
}
