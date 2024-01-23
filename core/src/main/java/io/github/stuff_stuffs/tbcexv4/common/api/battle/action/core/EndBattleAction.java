package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import net.minecraft.text.Text;

import java.util.Optional;

public class EndBattleAction implements BattleAction {
    public static final Codec<EndBattleAction> CODEC = Codec.unit(new EndBattleAction());

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActionTypes.END_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer, final BattleLogContext logContext) {
        try (final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(Optional.empty()), transactionContext)) {
            ((BattleStateImpl) state).finish(transactionContext, span);
        }
        logContext.accept(Text.of("Battle Ended!"));
        return true;
    }
}
