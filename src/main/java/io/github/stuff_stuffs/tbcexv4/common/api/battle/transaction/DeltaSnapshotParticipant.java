package io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

public abstract class DeltaSnapshotParticipant<T> implements BattleTransactionContext.CloseCallback {
    private final List<List<T>> snapshots = new ArrayList<>();

    protected abstract void revertDelta(T delta);

    protected void delta(final BattleTransactionContext transaction, final T delta) {
        while (snapshots.size() <= transaction.depth()) {
            snapshots.add(null);
        }
        final List<T> l;
        if (snapshots.get(transaction.depth()) != null) {
            l = snapshots.get(transaction.depth());
        } else {
            l = new ArrayList<>();
            snapshots.set(transaction.depth(), l);
            transaction.addCloseCallback(this);
        }
        l.add(delta);
    }

    @Override
    public void onClose(final BattleTransactionContext context, final TransactionContext.Result result) {
        if (result == TransactionContext.Result.ABORTED) {
            final List<T> list = snapshots.set(context.depth(), null);
            if (list != null) {
                final int s = list.size();
                for (int i = s - 1; i >= 0; i--) {
                    revertDelta(list.get(i));
                }
            }
        } else {
            if (context.depth() == 0) {
                return;
            }
            final List<T> list = snapshots.set(context.depth(), null);
            if (list == null) {
                return;
            }
            final List<T> prev = snapshots.get(context.depth() - 1);
            if (prev == null) {
                snapshots.set(context.depth() - 1, list);
                context.getOpenTransaction(context.depth() - 1).addCloseCallback(this);
            } else {
                prev.addAll(list);
            }
        }
    }
}
