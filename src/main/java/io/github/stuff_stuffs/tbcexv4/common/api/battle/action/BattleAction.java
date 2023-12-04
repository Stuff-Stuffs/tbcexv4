package io.github.stuff_stuffs.tbcexv4.common.api.battle.action;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;

public interface BattleAction {
    BattleActionType<?> type();

    void apply(BattleState state, BattleTracer tracer);

    static Codec<BattleAction> codec(final BattleCodecContext codecContext) {
        return Tbcexv4Registries.BattleActions.CODEC.dispatchStable(BattleAction::type, type -> type.codec(codecContext));
    }
}
