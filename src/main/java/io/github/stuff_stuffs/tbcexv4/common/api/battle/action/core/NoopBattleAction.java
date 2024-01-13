package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;

import java.util.Optional;
import java.util.UUID;

public class NoopBattleAction implements BattleAction {
    public static final Codec<NoopBattleAction> CODEC = Codec.optionalField("source", Uuids.CODEC).codec().xmap(NoopBattleAction::new, action -> action.source);
    private final Optional<UUID> source;

    public NoopBattleAction(final Optional<UUID> source) {
        this.source = source;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActions.NOOP_TYPE;
    }

    @Override
    public void apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer) {

    }

    @Override
    public Optional<BattleParticipantHandle> source() {
        return source.map(BattleParticipantHandle::new);
    }

    @Override
    public Text chatMessage() {
        return source.map(uuid -> Text.of(uuid + " passes!(Noop)")).orElseGet(() -> Text.of("Noop"));
    }
}
