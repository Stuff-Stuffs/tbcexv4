package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.ActionSource;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.text.Text;

import java.util.Optional;

public class NoopBattleAction implements BattleAction {
    public static final Codec<NoopBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.optionalFieldOf("source").forGetter(action -> action.source),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("energy").forGetter(action -> action.energy)
    ).apply(instance, NoopBattleAction::new));
    private final Optional<BattleParticipantHandle> source;
    private final int energy;

    public NoopBattleAction(final Optional<BattleParticipantHandle> source, final int energy) {
        this.source = source;
        this.energy = energy;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActionTypes.NOOP_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer, final BattleLogContext logContext) {
        if (logContext.enabled()) {
            if (source.isEmpty()) {
                logContext.accept(Text.of("Noop"));
            } else {
                logContext.accept(Text.of(source.get() + " passes with noop!"));
            }
        }
        return true;
    }

    @Override
    public Optional<ActionSource> source() {
        return source.map(handle -> new ActionSource(handle, energy));
    }
}
