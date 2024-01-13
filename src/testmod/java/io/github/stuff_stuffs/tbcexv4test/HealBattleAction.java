package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.text.Text;

import java.util.Optional;

public class HealBattleAction implements BattleAction {
    public static final Codec<HealBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.fieldOf("source").forGetter(action -> action.source),
            BattleParticipantHandle.CODEC.fieldOf("target").forGetter(action -> action.target)
    ).apply(instance, HealBattleAction::new));

    private final BattleParticipantHandle source;
    private final BattleParticipantHandle target;

    public HealBattleAction(final BattleParticipantHandle source, final BattleParticipantHandle target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Test.HEAL_TYPE;
    }

    @Override
    public void apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer) {
        try (final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(Optional.of(source)), transactionContext)) {
            state.participant(target).heal(1, transactionContext, span);
        }
    }

    @Override
    public Optional<BattleParticipantHandle> source() {
        return Optional.of(source);
    }

    @Override
    public Text chatMessage() {
        return Text.of(source.id() + " heals " + target.id());
    }
}
