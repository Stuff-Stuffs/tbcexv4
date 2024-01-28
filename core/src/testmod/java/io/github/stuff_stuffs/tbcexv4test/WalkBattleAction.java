package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

import java.util.Optional;

public class WalkBattleAction implements BattleAction {
    public static final Codec<WalkBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.fieldOf("actor").forGetter(o -> o.actor),
            Pather.PathNode.CODEC.fieldOf("pos").forGetter(o -> o.node)
    ).apply(instance, WalkBattleAction::new));
    private final BattleParticipantHandle actor;
    private final Pather.PathNode node;

    public WalkBattleAction(final BattleParticipantHandle actor, final Pather.PathNode node) {
        this.actor = actor;
        this.node = node;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Test.WALK_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> span) {
            try (final var nested = transactionContext.openNested()) {
                final BattleParticipant participant = state.participant(actor);
                final Result<Pather.PathNode, BattleParticipant.MoveError> result = participant.move(node, nested, span);
                if (result instanceof Result.Failure<Pather.PathNode, BattleParticipant.MoveError>) {
                    nested.abort();
                    return false;
                }
                nested.commit();
                return true;
            }
    }
}
