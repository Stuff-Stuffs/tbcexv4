package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantInitialState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAIControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;
import java.util.UUID;

public class PlayerJoinTestBattleAction implements BattleAction {
    public static final Codec<PlayerJoinTestBattleAction> CODEC = Uuids.STRING_CODEC.xmap(PlayerJoinTestBattleAction::new, action -> action.playerId);
    private final UUID playerId;

    public PlayerJoinTestBattleAction(final UUID id) {
        playerId = id;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Test.JOIN_TEST_TYPE;
    }

    @Override
    public void apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer) {
        try (final var transaction = transactionContext.openNested()) {
            final BattleTracer.Span<BattleTraceEvent> span = tracer.push(new BattleTraceEvent() {
            }, transaction);
            final BattleParticipantTeam playerTeam = new BattleParticipantTeam(playerId);
            final BattleParticipantTeam enemyTeam = new BattleParticipantTeam(MathHelper.randomUuid());
            final Result<BattleParticipantHandle, BattleState.AddParticipantError> result = state.addParticipant(new BattleParticipantInitialState() {
                @Override
                public Optional<UUID> id() {
                    return Optional.of(playerId);
                }

                @Override
                public BattleParticipantBounds bounds() {
                    return new BattleParticipantBounds(1, 1);
                }

                @Override
                public BattlePos pos() {
                    return new BattlePos(4, 4, 4);
                }

                @Override
                public BattleParticipantTeam team() {
                    return playerTeam;
                }

                @Override
                public void addAttachments(final BattleParticipantAttachment.Builder builder) {
                    builder.accept(new BattleParticipantPlayerControllerAttachment(playerId), Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
                }
            }, transaction, span);
            if (result instanceof Result.Failure<BattleParticipantHandle, BattleState.AddParticipantError>) {
                transaction.abort();
                return;
            } else {
                final BattleParticipant participant = state.participant(((Result.Success<BattleParticipantHandle, BattleState.AddParticipantError>) result).val());
                participant.stats().addStatModifier(Tbcexv4Registries.Stats.MAX_HEALTH, (currentValue, context) -> currentValue + 10, Tbcexv4Registries.StatModificationPhases.BASE_STATS, transaction, span);
                participant.heal(1000, transactionContext, span);
            }
            for (int i = 0; i < 8; i++) {
                if (!join(state, (i & 1) == 0 ? playerTeam : enemyTeam, transaction, span)) {
                    transaction.abort();
                    return;
                }
            }
            state.setRelation(playerTeam, enemyTeam, BattleParticipantTeamRelation.HOSTILE, transaction, span);
            span.close();
            transaction.commit();
        }
    }

    @Override
    public Text chatMessage() {
        return Text.of("Player " + playerId + " joining!");
    }

    private boolean join(final BattleState state, final BattleParticipantTeam team, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        final Result<BattleParticipantHandle, BattleState.AddParticipantError> result = state.addParticipant(new BattleParticipantInitialState() {
            @Override
            public BattleParticipantBounds bounds() {
                return new BattleParticipantBounds(1, 1);
            }

            @Override
            public BattlePos pos() {
                return new BattlePos(4, 4, 4);
            }

            @Override
            public BattleParticipantTeam team() {
                return team;
            }

            @Override
            public void addAttachments(final BattleParticipantAttachment.Builder builder) {
                builder.accept(new BattleParticipantAIControllerAttachment(), Tbcexv4Registries.BattleParticipantAttachmentTypes.AI_CONTROLLER);
            }
        }, transactionContext, tracer);
        if (result instanceof final Result.Success<BattleParticipantHandle, BattleState.AddParticipantError> success) {
            final BattleParticipant participant = state.participant(success.val());
            participant.stats().addStatModifier(Tbcexv4Registries.Stats.MAX_HEALTH, (currentValue, context) -> currentValue + 10, Tbcexv4Registries.StatModificationPhases.BASE_STATS, transactionContext, tracer);
            participant.heal(1000, transactionContext, tracer);
            return true;
        }
        return false;
    }
}
