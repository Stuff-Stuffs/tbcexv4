package io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

public abstract class DeltaSnapshotParticipant<T> implements BattleTransactionContext.CloseCallback {
    private final List<List<T>> deltas = new ArrayList<>();

    protected abstract void revertDelta(T delta);

    protected void delta(final BattleTransactionContext transaction, final T delta) {
        while (deltas.size() <= transaction.depth()) {
            deltas.add(null);
        }
        List<T> list = deltas.get(transaction.depth());
        if (list == null) {
            list = new ArrayList<>();
            deltas.set(transaction.depth(), list);
            transaction.addCloseCallback(this);
        }
        list.add(delta);
    }

    @Override
    public void onClose(final BattleTransactionContext context, final TransactionContext.Result result) {
        final List<T> list = deltas.set(context.depth(), null);
        if (list == null) {
            return;
        }
        if (result == TransactionContext.Result.ABORTED) {
            final int s = list.size();
            for (int i = s - 1; i >= 0; i--) {
                revertDelta(list.get(i));
            }
        } else {
            if (context.depth() == 0) {
                return;
            }
            final List<T> prev = deltas.get(context.depth() - 1);
            if (prev == null) {
                deltas.set(context.depth() - 1, list);
                context.outerTransaction().addCloseCallback(this);
            } else {
                prev.addAll(list);
            }
        }
    }
}
