package io.github.stuff_stuffs.tbcexv4.common.internal;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class Tbcexv4InternalEvents {
    public static final Event<BattleWatchEvent> BATTLE_WATCH_EVENT = EventFactory.createArrayBacked(BattleWatchEvent.class, events -> (prev, current, entity) -> {
        for (BattleWatchEvent event : events) {
            event.onWatch(prev, current, entity);
        }
    });

    public interface BattleWatchEvent {
        void onWatch(@Nullable BattleHandle prev, @Nullable BattleHandle current, ServerPlayerEntity entity);
    }

    private Tbcexv4InternalEvents() {
    }
}
