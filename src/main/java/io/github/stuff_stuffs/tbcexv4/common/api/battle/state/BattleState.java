package io.github.stuff_stuffs.tbcexv4.common.api.battle.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantInitialState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionManager;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

@EventViewable(viewClass = BattleStateView.class)
public interface BattleState extends BattleStateView {
    BattleTransactionManager transactionManager();

    @Override
    EventMap events();

    @Override
    BattleEnvironment environment();

    @Override
    BattleParticipant participant(BattleParticipantHandle handle);

    Result<Unit, RemoveParticipantError> removeParticipant(BattleParticipantHandle handle, RemoveParticipantReason reason, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    Result<BattleParticipantHandle, AddParticipantError> addParticipant(BattleParticipantInitialState battleParticipant, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    Result<Unit, SetBoundsError> setBounds(BattleBounds bounds, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    Result<Unit, SetTeamRelationError> setRelation(BattleParticipantTeam first, BattleParticipantTeam second, BattleParticipantTeamRelation relation, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    <T extends BattleAttachment> void setAttachment(T value, BattleAttachmentType<T> type, BattleTransactionContext transactionContext);

    enum AddParticipantError {
        OUT_OF_BOUNDS,
        ID_OVERLAP,
        EVENT,
        UNKNOWN
    }

    enum SetBoundsError {
        TOO_BIG,
        PARTICIPANT_OUTSIDE,
        EVENT
    }

    enum SetTeamRelationError {
        EVENT
    }

    enum RemoveParticipantError {
        EVENT,
        MISSING_PARTICIPANT
    }

    enum RemoveParticipantReason {
        DEAD,
        LEFT
    }
}
