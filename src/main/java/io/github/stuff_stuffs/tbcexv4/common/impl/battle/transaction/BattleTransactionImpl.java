package io.github.stuff_stuffs.tbcexv4.common.impl.battle.transaction;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionManager;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BattleTransactionImpl implements BattleTransaction {
    private final BattleTransactionManagerImpl manager;
    private final int depth;
    private final List<CloseCallback> callbacks = new ArrayList<>();
    private @Nullable TransactionContext.Result result = null;
    private BattleTransactionManager.Lifecycle lifecycle = BattleTransactionManager.Lifecycle.OPEN;

    public BattleTransactionImpl(final BattleTransactionManagerImpl manager, final int depth) {
        this.manager = manager;
        this.depth = depth;
    }

    @Override
    public void abort() {
        if (result != null || lifecycle != BattleTransactionManager.Lifecycle.OPEN) {
            throw new RuntimeException();
        }
        result = TransactionContext.Result.ABORTED;
    }

    @Override
    public void commit() {
        if (result != null || lifecycle != BattleTransactionManager.Lifecycle.OPEN) {
            throw new RuntimeException();
        }
        result = TransactionContext.Result.COMMITTED;
    }

    @Override
    public BattleTransaction openNested() {
        return manager.openNested(this);
    }

    @Override
    public void addCloseCallback(final CloseCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public BattleTransactionContext getOpenTransaction(final int nestingDepth) {
        return manager.atDepth(nestingDepth);
    }

    @Override
    public BattleTransactionManager.Lifecycle lifecycle() {
        return null;
    }

    @Override
    public void close() {
        if (lifecycle != BattleTransactionManager.Lifecycle.OPEN) {
            throw new RuntimeException();
        }
        if (result == null) {
            Tbcexv4.LOGGER.warn("Transaction wasn't committed or aborted before being closed!");
            abort();
        }
        lifecycle = BattleTransactionManager.Lifecycle.CLOSING;
        final int s = callbacks.size();
        for (int i = s - 1; i >= 0; i--) {
            callbacks.get(i).onClose(this, result);
        }
    }
}
