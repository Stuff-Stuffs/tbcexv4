package io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public interface BattleTransactionContext {
    BattleTransaction openNested();

    void addCloseCallback(CloseCallback callback);

    int depth();

    BattleTransactionContext outerTransaction();

    boolean closed();

    interface CloseCallback {
        void onClose(BattleTransactionContext context, TransactionContext.Result result);
    }
}
