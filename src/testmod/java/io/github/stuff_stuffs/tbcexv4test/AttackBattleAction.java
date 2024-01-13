package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.text.Text;

import java.util.Optional;

public class AttackBattleAction implements BattleAction {
    public static final Codec<AttackBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.fieldOf("source").forGetter(action -> action.source),
            BattleParticipantHandle.CODEC.fieldOf("target").forGetter(action -> action.target)
    ).apply(instance, AttackBattleAction::new));

    private final BattleParticipantHandle source;
    private final BattleParticipantHandle target;

    public AttackBattleAction(final BattleParticipantHandle source, final BattleParticipantHandle target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Test.ATTACK_TYPE;
    }

    @Override
    public void apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer) {
        try (final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(Optional.of(source)), transactionContext)) {
            state.participant(target).damage(4, Tbcexv4Registries.DamageTypes.ROOT, transactionContext, span);
        }
    }

    @Override
    public Optional<BattleParticipantHandle> source() {
        return Optional.of(source);
    }

    @Override
    public Text chatMessage() {
        return Text.of(source.id() + " attacks " + target.id());
    }
}
