package io.github.stuff_stuffs.tbcexv4.client.api;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.Optional;
import java.util.function.Function;

public interface WatchedBattleChangeEvent {
    Event<WatchedBattleChangeEvent> EVENT = EventFactory.createArrayBacked(WatchedBattleChangeEvent.class, events -> handle -> {
        for (WatchedBattleChangeEvent event : events) {
            event.onWatchedBattleChanged(handle);
        }
    });

    void onWatchedBattleChanged(Optional<BattleHandle> handle);
}
