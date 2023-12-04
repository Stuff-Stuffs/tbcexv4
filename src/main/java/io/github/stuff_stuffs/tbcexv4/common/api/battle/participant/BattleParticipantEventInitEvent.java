package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface BattleParticipantEventInitEvent {
    Event<BattleParticipantEventInitEvent> EVENT = EventFactory.createArrayBacked(BattleParticipantEventInitEvent.class, events -> builder -> {
        for (BattleParticipantEventInitEvent event : events) {
            event.addEvents(builder);
        }
    });

    void addEvents(EventMap.Builder builder);
}
