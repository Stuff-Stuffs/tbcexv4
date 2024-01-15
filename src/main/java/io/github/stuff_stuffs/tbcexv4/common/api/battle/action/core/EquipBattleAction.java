package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.Inventory;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.text.Text;

import java.util.Optional;

public class EquipBattleAction implements BattleAction {
    public static final Codec<EquipBattleAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BattleParticipantHandle.CODEC.fieldOf("actor").forGetter(o -> o.actor),
            InventoryHandle.CODEC.fieldOf("invSlot").forGetter(o -> o.inventoryHandle),
            Tbcexv4Registries.EquipmentSlots.CODEC.fieldOf("slot").forGetter(o -> o.slot)
    ).apply(instance, EquipBattleAction::new));
    private final BattleParticipantHandle actor;
    private final InventoryHandle inventoryHandle;
    private final EquipmentSlot slot;

    public EquipBattleAction(final BattleParticipantHandle actor, final InventoryHandle handle, final EquipmentSlot slot) {
        this.actor = actor;
        inventoryHandle = handle;
        this.slot = slot;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActionTypes.EQUIP_TYPE;
    }

    @Override
    public boolean apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer, final BattleLogContext logContext) {
        try (final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(Optional.of(actor)), transactionContext)) {
            try (final var nested = transactionContext.openNested()) {
                final boolean logEnabled = logContext.enabled();
                if (logEnabled) {
                    logContext.accept(Text.of(actor + " trying to equip item!"));
                    logContext.pushIndent();
                }
                final BattleParticipant participant = state.participant(actor);
                final Inventory.InventoryEntry entry = participant.inventory().get(inventoryHandle);
                final Optional<BattleItemStack> opt = entry.stack();
                if (opt.isEmpty()) {
                    if (logEnabled) {
                        logContext.accept(Text.of("Failed! The inventory slot was empty"));
                        logContext.popIndent();
                    }
                    nested.abort();
                    return false;
                }
                final BattleItem item = opt.get().item();
                final Result<Integer, Inventory.TakeError> takeResult = entry.take(1, nested, span);
                if (takeResult instanceof Result.Failure<Integer, Inventory.TakeError>) {
                    if (logEnabled) {
                        logContext.accept(Text.of("Failed! Could not take item!"));
                        logContext.popIndent();
                    }
                    nested.abort();
                    return false;
                }
                final Result<Unit, Inventory.EquipError> equipResult = participant.inventory().equip(item, slot, nested, span);
                if (equipResult instanceof Result.Failure<Unit, Inventory.EquipError>) {
                    if (logEnabled) {
                        logContext.accept(Text.of("Failed! Could not equip item!"));
                        logContext.popIndent();
                    }
                    nested.abort();
                    return false;
                }
                nested.commit();
                if (logEnabled) {
                    logContext.accept(Text.of("Success!"));
                    logContext.popIndent();
                }
                return true;
            }
        }
    }
}
