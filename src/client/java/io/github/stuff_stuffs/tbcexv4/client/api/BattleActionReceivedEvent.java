package io.github.stuff_stuffs.tbcexv4.client.api;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.function.Function;

public interface BattleActionReceivedEvent {
    Event<BattleActionReceivedEvent> EVENT = EventFactory.createArrayBacked(BattleActionReceivedEvent.class, events -> battle -> {
        for (BattleActionReceivedEvent event : events) {
            event.onBattleActionReceived(battle);
        }
    });

    void onBattleActionReceived(BattleView battle);
}
