package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.ActionSearchStrategy;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.Scorer;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.Scorers;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantInitialState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAIControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerJoinTestBattleAction implements BattleAction {
    public static final Codec<PlayerJoinTestBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.CODEC.fieldOf("playerId").forGetter(action -> action.playerId),
            Codec.intRange(0, BattlePos.MAX).fieldOf("x").forGetter(action -> action.x),
            Codec.intRange(0, BattlePos.MAX).fieldOf("y").forGetter(action -> action.y),
            Codec.intRange(0, BattlePos.MAX).fieldOf("z").forGetter(action -> action.z),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(o -> o.entries)
    ).apply(instance, PlayerJoinTestBattleAction::new));
    private final UUID playerId;
    private final int x;
    private final int y;
    private final int z;
    private final List<Entry> entries;

    public PlayerJoinTestBattleAction(final UUID id, final int x, final int y, final int z, final List<Entry> entries) {
        playerId = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.entries = entries;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Test.JOIN_TEST_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer, final BattleLogContext logContext) {
        try (final var transaction = transactionContext.openNested()) {
            final BattleTracer.Span<BattleTraceEvent> span = tracer.push(new BattleTraceEvent() {
            }, transaction);
            final BattleParticipantTeam playerTeam = new BattleParticipantTeam(playerId);
            final Random random = new Xoroshiro128PlusPlusRandom(playerId.getLeastSignificantBits(), playerId.getMostSignificantBits());
            final BattleParticipantTeam enemyTeam = new BattleParticipantTeam(new UUID(random.nextLong(), random.nextLong()));
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
                    return new BattlePos(x, y, z);
                }

                @Override
                public BattleParticipantTeam team() {
                    return playerTeam;
                }

                @Override
                public void addAttachments(final BattleParticipantAttachment.Builder builder) {
                    builder.accept(new BattleParticipantPlayerControllerAttachment(playerId), Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
                    builder.accept(new RenderDataParticipantAttachment(RenderDataParticipantAttachmentView.Type.PLAYER), Tbcexv4Test.RENDER_DATA_ATTACHMENT);

                }
            }, transaction, span);
            if (result instanceof Result.Failure<BattleParticipantHandle, BattleState.AddParticipantError>) {
                transaction.abort();
                return false;
            } else {
                final BattleParticipant participant = state.participant(((Result.Success<BattleParticipantHandle, BattleState.AddParticipantError>) result).val());
                participant.stats().addStatModifier(Tbcexv4Registries.Stats.MAX_HEALTH, (currentValue, context) -> currentValue + 10, Tbcexv4Registries.StatModificationPhases.BASE_STATS, transaction, span);
                participant.heal(1000, transactionContext, span);
                participant.inventory().give(new BattleItemStack(new TestBattleItem(), 10), transactionContext, span);
            }
            for (final Entry entry : entries) {
                if (!join(state, entry.enemy ? enemyTeam : playerTeam, entry.pos, transaction, span, entry.enemy ? RenderDataParticipantAttachmentView.Type.SHEEP : RenderDataParticipantAttachmentView.Type.PIG)) {
                    transaction.abort();
                    return false;
                }
            }
            state.setRelation(playerTeam, enemyTeam, BattleParticipantTeamRelation.HOSTILE, transaction, span);
            span.close();
            transaction.commit();
            return true;
        }
    }

    private boolean join(final BattleState state, final BattleParticipantTeam team, final BattlePos pos, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer, final RenderDataParticipantAttachmentView.Type type) {
        final Result<BattleParticipantHandle, BattleState.AddParticipantError> result = state.addParticipant(new BattleParticipantInitialState() {
            @Override
            public BattleParticipantBounds bounds() {
                return new BattleParticipantBounds(1, 1);
            }

            @Override
            public BattlePos pos() {
                return pos;
            }

            @Override
            public BattleParticipantTeam team() {
                return team;
            }

            @Override
            public void addAttachments(final BattleParticipantAttachment.Builder builder) {
                builder.accept(new BattleParticipantAIControllerAttachment(f -> ActionSearchStrategy.basic(1.0, Scorer.sum(Scorers.health(f.handle()), Scorers.enemyTeamHealth(f.handle())))), Tbcexv4Registries.BattleParticipantAttachmentTypes.AI_CONTROLLER);
                builder.accept(new RenderDataParticipantAttachment(type), Tbcexv4Test.RENDER_DATA_ATTACHMENT);
            }
        }, transactionContext, tracer);
        if (result instanceof final Result.Success<BattleParticipantHandle, BattleState.AddParticipantError> success) {
            final BattleParticipant participant = state.participant(success.val());
            participant.stats().addStatModifier(Tbcexv4Registries.Stats.MAX_HEALTH, (currentValue, context) -> currentValue + 10, Tbcexv4Registries.StatModificationPhases.BASE_STATS, transactionContext, tracer);
            participant.heal(1000, transactionContext, tracer);
            participant.inventory().give(new BattleItemStack(new TestBattleItem(), 2), transactionContext, tracer);
            return true;
        }
        return false;
    }

    public record Entry(BattlePos pos, boolean enemy) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BattlePos.CODEC.fieldOf("pos").forGetter(o -> o.pos),
                Codec.BOOL.fieldOf("enemy").forGetter(o -> o.enemy)
        ).apply(instance, Entry::new));
    }
}
