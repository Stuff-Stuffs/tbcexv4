package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;

@EventViewable(viewClass = InventoryView.class)
public interface Inventory extends InventoryView {
    @Override
    InventoryEntry get(InventoryHandle handle);

    @Override
    Iterable<? extends InventoryEntry> entries();

    Result<InventoryEntry, GiveError> give(BattleItemStack stack, BattleTracer.Span<?> tracer);

    interface InventoryEntry extends InventoryEntryView {
        Result<Unit, GiveError> set(BattleItemStack stack, BattleTracer.Span<?> tracer);

        Result<Integer, TakeError> take(int amount, BattleTracer.Span<?> tracer);
    }

    enum GiveError {
        EVENT
    }

    enum TakeError {
        EVENT,
        EMPTY_STACK
    }
}
