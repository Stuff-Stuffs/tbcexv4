package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

import java.util.Optional;
import java.util.UUID;

public interface BattleParticipantInitialState {
    BattleParticipantBounds bounds();

    BattlePos pos();

    BattleParticipantTeam team();

    void addAttachments(BattleParticipantAttachment.Builder builder);

    default Optional<UUID> id() {
        return Optional.empty();
    }

    default void initialize(final BattleState state, final BattleParticipant participant, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
    }
}
