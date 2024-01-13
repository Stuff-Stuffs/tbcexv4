package io.github.stuff_stuffs.tbcexv4.common.api.event;

import io.github.stuff_stuffs.event_gen.api.event.EventKey;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

public interface EventMapView {
    <View> Token registerView(EventKey<?, View> key, View view, BattleTransactionContext transactionContext);

    <View> Token registerTerminalView(EventKey<?, View> key, View view);

    boolean contains(EventKey<?, ?> key);

    interface Token {
        boolean alive();

        void kill(BattleTransactionContext transactionContext);
    }
}
