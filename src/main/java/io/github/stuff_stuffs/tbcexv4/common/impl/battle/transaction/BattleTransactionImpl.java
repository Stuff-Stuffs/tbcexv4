package io.github.stuff_stuffs.tbcexv4.common.impl.battle.transaction;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
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
    private boolean closed = false;

    public BattleTransactionImpl(final BattleTransactionManagerImpl manager, final int depth) {
        this.manager = manager;
        this.depth = depth;
    }

    @Override
    public void abort() {
        if (result != null || closed()) {
            throw new RuntimeException();
        }
        result = TransactionContext.Result.ABORTED;
    }

    @Override
    public void commit() {
        if (result != null || closed()) {
            throw new RuntimeException();
        }
        result = TransactionContext.Result.COMMITTED;
    }

    @Override
    public BattleTransaction openNested() {
        if (closed()) {
            throw new RuntimeException();
        }
        return manager.openNested(this);
    }

    @Override
    public void addCloseCallback(final CloseCallback callback) {
        if (closed()) {
            throw new RuntimeException();
        }
        callbacks.add(callback);
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public BattleTransactionContext outerTransaction() {
        return manager.atDepth(depth - 1);
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed()) {
            throw new RuntimeException();
        }
        if (result == null) {
            Tbcexv4.LOGGER.debug("Transaction wasn't committed or aborted before being closed!");
            abort();
        }
        closed = true;
        final int s = callbacks.size();
        for (int i = s - 1; i >= 0; i--) {
            callbacks.get(i).onClose(this, result);
        }
    }
}
