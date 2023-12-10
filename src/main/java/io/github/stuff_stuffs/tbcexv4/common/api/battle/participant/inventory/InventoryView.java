package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;

import java.util.Optional;

public interface InventoryView {
    InventoryEntryView get(InventoryHandle handle);

    Iterable<? extends InventoryEntryView> entries();

    Optional<? extends BattleItem> equippedItem(EquipmentSlot slot);

    Optional<? extends EquipmentView> equipment(EquipmentSlot slot);

    interface InventoryEntryView {
        InventoryHandle handle();

        Optional<BattleItemStack> stack();
    }
}
