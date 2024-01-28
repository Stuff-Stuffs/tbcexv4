package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.Inventory;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

public class UnequipBattleAction implements BattleAction {
    public static final Codec<UnequipBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.fieldOf("actor").forGetter(o -> o.actor),
            Tbcexv4Registries.EquipmentSlots.CODEC.fieldOf("slot").forGetter(o -> o.slot)
    ).apply(instance, UnequipBattleAction::new));
    private final BattleParticipantHandle actor;
    private final EquipmentSlot slot;

    public UnequipBattleAction(final BattleParticipantHandle actor, final EquipmentSlot slot) {
        this.actor = actor;
        this.slot = slot;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActionTypes.UNEQUIP_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> span) {
        try (final var nested = transactionContext.openNested()) {
            final BattleParticipant participant = state.participant(actor);
            final Result<InventoryHandle, Inventory.UnequipError> result = participant.inventory().unequip(slot, nested, span);
            if (result instanceof Result.Failure<InventoryHandle, Inventory.UnequipError>) {
                nested.abort();
                return false;
            }
            nested.commit();
            return true;
        }
    }
}
