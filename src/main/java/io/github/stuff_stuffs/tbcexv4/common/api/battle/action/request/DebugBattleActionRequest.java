package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;

public record DebugBattleActionRequest(BattleAction action) implements BattleActionRequest {
    @Override
    public BattleActionRequestType<?> type() {
        return Tbcexv4Registries.BattleActionRequestTypes.DEBUG_TYPE;
    }

    public static Codec<DebugBattleActionRequest> codec(final BattleCodecContext context) {
        return BattleAction.codec(context).xmap(DebugBattleActionRequest::new, DebugBattleActionRequest::action);
    }
}
