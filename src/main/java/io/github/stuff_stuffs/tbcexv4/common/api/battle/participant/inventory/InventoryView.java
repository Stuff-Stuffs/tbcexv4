package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;

import java.util.Optional;

public interface InventoryView {
    InventoryEntryView get(InventoryHandle handle);

    Iterable<? extends InventoryEntryView> entries();

    interface InventoryEntryView {
        InventoryHandle handle();

        Optional<BattleItemStack> stack();
    }
}
