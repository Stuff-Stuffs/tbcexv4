package io.github.stuff_stuffs.tbcexv4.common.api.event;

import io.github.stuff_stuffs.event_gen.api.event.EventKey;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.impl.event.EventMapBuilderImpl;

public interface EventMap extends EventMapView {
    <Mut> Token registerMut(EventKey<Mut, ?> key, Mut event, BattleTransactionContext transactionContext);

    <Mut> Mut invoker(EventKey<Mut, ?> key);

    static Builder builder() {
        return new EventMapBuilderImpl();
    }

    interface Builder {
        <Mut, View> Builder add(EventKey<Mut, View> key, EventKey.Factory<Mut, View> factory);

        EventMap build();
    }
}
