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
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory.item.UnknownBattleItem;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.item.Items;
import net.minecraft.util.Uuids;

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
                    return new BattleParticipantTeam(playerId);
                }

                @Override
                public void addAttachments(final BattleParticipantAttachment.Builder builder) {
                    builder.accept(new BattleParticipantPlayerControllerAttachment(playerId), Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
                }
            }, transaction, span);
            if (result instanceof final Result.Success<BattleParticipantHandle, BattleState.AddParticipantError> success) {
                for (int i = 0; i < 4; i++) {
                    final BattleParticipant participant = state.participant(success.val());
                    BattleItemStack stack = new BattleItemStack(new UnknownBattleItem(Items.ANVIL, Optional.empty(), IntOpenHashSet.of(0)), 4);
                    participant.inventory().give(stack, transaction, span);
                    stack = new BattleItemStack(new UnknownBattleItem(Items.STONE, Optional.empty(), IntOpenHashSet.of(0)), 2);
                    participant.inventory().give(stack, transaction, span);
                    stack = new BattleItemStack(new UnknownBattleItem(Items.LADDER, Optional.empty(), IntOpenHashSet.of(0)), 15);
                    participant.inventory().give(stack, transaction, span);
                    stack = new BattleItemStack(new UnknownBattleItem(Items.BONE, Optional.empty(), IntOpenHashSet.of(0)), 33);
                    participant.inventory().give(stack, transaction, span);
                }
                transaction.commit();
            }
        }
    }
}
