package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.Inventory;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatContainer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@EventViewable(viewClass = BattleParticipantView.class)
public interface BattleParticipant extends BattleParticipantView {
    @Override
    EventMap events();

    @Override
    BattleState battleState();

    @Override
    StatContainer stats();

    @Override
    Inventory inventory();

    Result<Unit, SetBoundsError> setBounds(BattleParticipantBounds bounds, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    Result<Unit, SetPosError> setPos(BattlePos pos, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    double damage(double amount, DamageType type, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    double heal(double amount, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    double setHealth(double amount, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    <T extends BattleParticipantAttachment> Optional<T> attachment(BattleParticipantAttachmentType<?, T> type);

    <T extends BattleParticipantAttachment> void setAttachment(@Nullable T value, BattleParticipantAttachmentType<?, T> type, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    enum SetBoundsError {
        EVENT,
        OUTSIDE_BATTLE
    }

    enum SetPosError {
        EVENT,
        OUTSIDE_BATTLE,
        ENV_COLLISION
    }
}
