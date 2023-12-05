package io.github.stuff_stuffs.tbcexv4.common.impl.battle.transaction;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionManager;

import java.util.ArrayList;
import java.util.List;

public class BattleTransactionManagerImpl implements BattleTransactionManager {
    private final List<BattleTransactionImpl> stack = new ArrayList<>();

    @Override
    public BattleTransaction open() {
        if (isOpen()) {
            throw new RuntimeException();
        }
        final BattleTransactionImpl transaction = new BattleTransactionImpl(this, 0);
        stack.add(transaction);
        return transaction;
    }

    @Override
    public boolean isOpen() {
        return !stack.isEmpty();
    }

    public BattleTransaction openNested(final BattleTransactionImpl transaction) {
        if (stack.isEmpty() || stack.get(stack.size() - 1) != transaction) {
            throw new RuntimeException();
        }
        final BattleTransactionImpl next = new BattleTransactionImpl(this, transaction.depth() + 1);
        stack.add(next);
        return next;
    }

    public BattleTransactionContext atDepth(final int depth) {
        return stack.get(depth);
    }
}
