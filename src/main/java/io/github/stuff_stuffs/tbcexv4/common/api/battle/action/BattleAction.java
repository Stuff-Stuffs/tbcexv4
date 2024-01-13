package io.github.stuff_stuffs.tbcexv4.common.api.battle.action;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.text.Text;

import java.util.Optional;

public interface BattleAction {
    BattleActionType<?> type();

    void apply(BattleState state, BattleTransactionContext transactionContext, BattleTracer tracer);

    default Optional<BattleParticipantHandle> source() {
        return Optional.empty();
    }

    Text chatMessage();

    static Codec<BattleAction> codec(final BattleCodecContext codecContext) {
        return Tbcexv4Registries.BattleActions.CODEC.dispatchStable(BattleAction::type, type -> type.codec(codecContext));
    }
}
