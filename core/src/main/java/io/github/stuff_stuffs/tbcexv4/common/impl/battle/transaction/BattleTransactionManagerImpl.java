package io.github.stuff_stuffs.tbcexv4.common.impl.battle.transaction;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BattleTransactionManagerImpl implements BattleTransactionManager {
    private final AtomicBoolean open = new AtomicBoolean(false);
    private final List<BattleTransactionImpl> stack = new ArrayList<>();

    @Override
    public BattleTransaction open() {
        if (open.compareAndExchange(false, true)) {
            throw new RuntimeException();
        }
        final BattleTransactionImpl transaction = new BattleTransactionImpl(this, 0);
        stack.add(transaction);
        return transaction;
    }

    public void pop(final int depth) {
        if (stack.size() == depth + 1) {
            stack.remove(depth);
            if (stack.isEmpty()) {
                open.setRelease(false);
            }
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public boolean isOpen() {
        return open.getAcquire();
    }

    public BattleTransaction openNested(final BattleTransactionImpl transaction) {
        if (stack.isEmpty() || stack.get(stack.size() - 1) != transaction) {
            throw new RuntimeException();
        }
        final BattleTransactionImpl next = new BattleTransactionImpl(this, transaction.depth() + 1);
        stack.add(next);
        return next;
    }

    public BattleTransactionImpl atDepth(final int depth) {
        return stack.get(depth);
    }
}
