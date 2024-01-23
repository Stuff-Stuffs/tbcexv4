package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.Equipment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

import java.util.Optional;
import java.util.Set;

@EventViewable(viewClass = InventoryView.class)
public interface Inventory extends InventoryView {
    @Override
    InventoryEntry get(InventoryHandle handle);

    @Override
    Iterable<? extends InventoryEntry> entries();

    Result<InventoryEntry, GiveError> give(BattleItemStack stack, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    Result<Unit, EquipError> equip(BattleItem item, EquipmentSlot slot, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    Result<InventoryHandle, UnequipError> unequip(EquipmentSlot slot, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    @Override
    Optional<? extends Equipment> equipment(EquipmentSlot slot);

    interface InventoryEntry extends InventoryEntryView {
        Result<Unit, GiveError> set(BattleItemStack stack, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

        Result<Integer, TakeError> take(int amount, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);
    }

    enum GiveError {
        EVENT
    }

    enum TakeError {
        EVENT,
        EMPTY_STACK
    }

    sealed interface EquipError {
        record Event() implements EquipError {
        }

        record InvalidItem() implements EquipError {
        }

        record Blocked(Set<EquipmentSlot> slots) implements EquipError {
        }

        record SlotFilled() implements EquipError {
        }
    }

    sealed interface UnequipError {
        record Event() implements UnequipError {
        }

        record InventoryGive(GiveError error) implements UnequipError {
        }

        record SlotEmpty() implements UnequipError {
        }
    }
}
