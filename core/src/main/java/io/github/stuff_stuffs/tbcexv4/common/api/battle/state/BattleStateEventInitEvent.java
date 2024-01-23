package io.github.stuff_stuffs.tbcexv4.common.api.battle.state;

import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface BattleStateEventInitEvent {
    Event<BattleStateEventInitEvent> EVENT = EventFactory.createArrayBacked(BattleStateEventInitEvent.class, events -> builder -> {
        for (BattleStateEventInitEvent event : events) {
            event.addEvents(builder);
        }
    });

    void addEvents(EventMap.Builder builder);
}
