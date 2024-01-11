package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import com.google.common.collect.Iterators;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;

public abstract class AbstractTargetChooser<I, T extends Target> implements TargetChooser<T> {
    protected final Plan parent;
    protected final BattleStateView state;

    protected AbstractTargetChooser(final Plan parent, final BattleStateView state) {
        this.parent = parent;
        this.state = state;
    }

    protected abstract I extract(T target);

    protected abstract Iterator<? extends I> iterator();

    protected abstract double weight0(I obj, double temperature, final Random random, final BattleTransactionContext context);

    protected abstract T create(I obj);

    @Override
    public Iterator<? extends T> all() {
        return Iterators.transform(iterator(), this::create);
    }

    protected I choose0(final double temperature, final Random random, final BattleTransactionContext context) {
        double wSum = 0;
        I chosen = null;
        final Iterator<? extends I> iterator = iterator();
        while (iterator.hasNext()) {
            final I next = iterator.next();
            final double weight = Math.exp(-1 / (weight0(next, temperature, random, context) + 0.1) / temperature) + 0.000001;
            if (chosen == null) {
                chosen = next;
                wSum = weight;
            } else {
                wSum = wSum + weight;
                final double fraction = weight / wSum;
                if (random.nextDouble() <= fraction) {
                    chosen = next;
                }
            }
        }
        if (chosen == null) {
            throw new IllegalStateException();
        }
        return chosen;
    }

    @Override
    public Plan parent() {
        return parent;
    }

    @Override
    public T choose(final double temperature, final Random random, final BattleTransactionContext context) {
        return create(choose0(temperature, random, context));
    }

    @Override
    public double weight(final T target, final double temperature, final Random random, final BattleTransactionContext context) {
        return weight0(extract(target), temperature, random, context);
    }
}
