package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import net.minecraft.text.Text;

import java.util.Optional;

public class WalkBattleAction implements BattleAction {
    public static final Codec<WalkBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.fieldOf("actor").forGetter(o -> o.actor),
            BattlePos.CODEC.fieldOf("pos").forGetter(o -> o.pos)
    ).apply(instance, WalkBattleAction::new));
    private final BattleParticipantHandle actor;
    private final BattlePos pos;

    public WalkBattleAction(final BattleParticipantHandle actor, final BattlePos pos) {
        this.actor = actor;
        this.pos = pos;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Test.WALK_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer, final BattleLogContext logContext) {
        try (final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(Optional.of(actor)), transactionContext)) {
            try (final var nested = transactionContext.openNested()) {
                final BattleParticipant participant = state.participant(actor);
                final Result<Unit, BattleParticipant.SetPosError> result = participant.setPos(pos, nested, span);
                if (result instanceof Result.Failure<Unit, BattleParticipant.SetPosError>) {
                    nested.abort();
                    return false;
                }
                if (logContext.enabled()) {
                    logContext.accept(Tbcexv4Util.concat(Text.of(actor.id()), Text.of(" walks to "), Text.of(pos.toString())));
                }
                nested.commit();
                return true;
            }
        }
    }
}
