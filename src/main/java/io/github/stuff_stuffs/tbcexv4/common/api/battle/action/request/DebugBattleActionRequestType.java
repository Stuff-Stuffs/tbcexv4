package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DebugBattleActionRequestType implements BattleActionRequestType<DebugBattleActionRequest> {
    @Override
    public Result<Unit, Text> check(final DebugBattleActionRequest request, final ServerPlayerEntity source, final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        return source.hasPermissionLevel(2) ? new Result.Success<>(Unit.INSTANCE): new Result.Failure<>(Text.of("Must be operator to use debug battle action requests!"));
    }

    @Override
    public Codec<DebugBattleActionRequest> codec(final BattleCodecContext context) {
        return DebugBattleActionRequest.codec(context);
    }

    @Override
    public BattleAction extract(final DebugBattleActionRequest request) {
        return request.action();
    }
}
