package io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;

@EventViewable(viewClass = BattleTransactionContext.class)
public interface BattleTransaction extends AutoCloseable, BattleTransactionContext {
    void abort();

    void commit();

    @Override
    void close();
}
