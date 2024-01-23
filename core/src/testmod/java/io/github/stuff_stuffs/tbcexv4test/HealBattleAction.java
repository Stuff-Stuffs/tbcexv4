package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.ActionSource;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import net.minecraft.text.Text;

import java.util.Optional;

public class HealBattleAction implements BattleAction {
    public static final Codec<HealBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.fieldOf("actor").forGetter(o -> o.actor),
            BattleParticipantHandle.CODEC.fieldOf("target").forGetter(o -> o.target)
    ).apply(instance, HealBattleAction::new));
    private final BattleParticipantHandle actor;
    private final BattleParticipantHandle target;

    public HealBattleAction(final BattleParticipantHandle actor, final BattleParticipantHandle target) {
        this.actor = actor;
        this.target = target;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Test.HEAL_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer, final BattleLogContext logContext) {
        try (final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(Optional.of(actor)), transactionContext)) {
            try (final var nested = transactionContext.openNested()) {
                final BattleParticipant participant = state.participant(actor);
                final BattleParticipant targetParticipant = state.participant(target);
                if (BattleParticipantBounds.distance2(participant.bounds(), participant.pos(), targetParticipant.bounds(), targetParticipant.pos()) > 2) {
                    nested.abort();
                    return false;
                }
                final double healedAmount = targetParticipant.heal(3, nested, span);
                if (healedAmount <= 0.0) {
                    nested.abort();
                    return false;
                }
                if (logContext.enabled()) {
                    logContext.accept(Tbcexv4Util.concat(Text.of(actor.id()), Text.of(" heals "), Text.of(target.id()), Text.of(" for " + healedAmount + "health!")));
                }
                nested.commit();
                return true;
            }
        }
    }

    @Override
    public Optional<ActionSource> source() {
        return Optional.of(new ActionSource(actor, 1));
    }
}