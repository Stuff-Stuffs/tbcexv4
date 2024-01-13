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
import org.jetbrains.annotations.Nullable;

public interface BattleActionRequestType<T extends BattleActionRequest> {
    Result<Unit, Text> check(final T request, @Nullable ServerPlayerEntity source, final BattleState state, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    Codec<T> codec(BattleCodecContext context);

    BattleAction extract(T request);
}
