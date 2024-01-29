package io.github.stuff_stuffs.tbcexv4.common.impl.battle.transaction;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class BattleTransactionManagerImpl implements BattleTransactionManager {

    private final AtomicStack stack;

    public BattleTransactionManagerImpl() {
        stack = new AtomicStack();
    }

    @Override
    public BattleTransaction open() {
        final BattleTransactionImpl transaction = new BattleTransactionImpl(this, 0);
        final int depth = stack.depth.getAcquire();
        if (depth != 0) {
            throw new RuntimeException();
        }
        final BattleTransactionImpl old = stack.stack.compareAndExchangeRelease(0, null, transaction);
        if (old != null) {
            throw new RuntimeException();
        }
        final int oldDepth = stack.depth.compareAndExchangeRelease(0, 1);
        if (oldDepth != 0) {
            throw new RuntimeException();
        }
        return transaction;
    }

    public void pop(final int depth) {
        final int stackDepth = stack.depth.getAcquire();
        if (stackDepth == depth + 1) {
            final BattleTransactionImpl old = stack.stack.getAndSet(depth, null);
            if (old == null) {
                throw new RuntimeException();
            }
            final int oldDepth = stack.depth.compareAndExchange(stackDepth, stackDepth - 1);
            if (oldDepth != stackDepth) {
                throw new RuntimeException();
            }
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public boolean isOpen() {
        return stack.depth.getAcquire() != 0;
    }

    public BattleTransaction openNested(final BattleTransactionImpl transaction) {
        int depth;
        BattleTransactionImpl next;
        do {
            depth = stack.depth.getAcquire();
            if (depth != transaction.depth() + 1) {
                throw new RuntimeException();
            }
            next = new BattleTransactionImpl(this, depth);
        } while (stack.stack.compareAndExchange(depth, null, next) != null);
        stack.depth.setRelease(depth + 1);
        return next;
    }

    public BattleTransactionImpl atDepth(final int depth) {
        return stack.stack.getAcquire(depth);
    }

    private static final class AtomicStack {
        private final AtomicReferenceArray<BattleTransactionImpl> stack;
        private final AtomicInteger depth;

        private AtomicStack() {
            stack = new AtomicReferenceArray<>(8192);
            depth = new AtomicInteger(0);
        }
    }
}
