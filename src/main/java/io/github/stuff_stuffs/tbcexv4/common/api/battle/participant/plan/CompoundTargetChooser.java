package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import com.google.common.collect.Iterators;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;
import java.util.List;

public class CompoundTargetChooser<T extends Target> implements TargetChooser<T> {
    private final List<TargetChooser<T>> choosers;
    private final Wrapper<T> wrapper;
    private final double weight;

    public CompoundTargetChooser(final List<TargetChooser<T>> choosers, final Wrapper<T> wrapper, final double weight) {
        if (choosers.isEmpty()) {
            throw new IllegalArgumentException();
        }
        final TargetChooser<T> chooser = choosers.get(0);
        for (final TargetChooser<T> targetChooser : choosers) {
            if (targetChooser.type() != chooser.type() || targetChooser.parent() != chooser.parent()) {
                throw new IllegalStateException();
            }
        }
        this.choosers = choosers;
        this.wrapper = wrapper;
        this.weight = weight;
    }

    @Override
    public Plan parent() {
        return choosers.get(0).parent();
    }

    @Override
    public TargetType<T> type() {
        return choosers.get(0).type();
    }

    @Override
    public Iterator<? extends T> all() {
        final Iterator<? extends T>[] arr = new Iterator[choosers.size()];
        int idx = 0;
        for (final TargetChooser<T> chooser : choosers) {
            arr[idx++] = chooser.all();
        }
        return Iterators.concat(arr);
    }

    @Override
    public T choose(final double temperature, final Random random, final BattleTransactionContext context) {
        double wSum = 0;
        T chosen = null;
        for (final TargetChooser<T> chooser : choosers) {
            final T sub = chooser.choose(temperature, random, context);
            final double weight = Math.exp(-1 / chooser.weight(sub, temperature, random, context) / temperature) + 0.000001;
            if (chosen == null) {
                chosen = wrapper.wrap(sub, this, chooser);
                wSum = weight;
            } else {
                wSum = wSum + weight;
                final double fraction = weight / wSum;
                if (random.nextDouble() <= fraction) {
                    chosen = wrapper.wrap(sub, this, chooser);
                }
            }
        }
        if (chosen == null) {
            throw new IllegalStateException();
        }
        return chosen;
    }

    @Override
    public double weight(final T target, final double temperature, final Random random, final BattleTransactionContext context) {
        final TargetChooser<T> tag = wrapper.tag(target);
        for (final TargetChooser<T> chooser : choosers) {
            if (tag == chooser) {
                return chooser.weight(wrapper.unwrap(target), temperature, random, context);
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public double weight() {
        return weight;
    }

    public interface Wrapper<T extends Target> {
        T wrap(T val, TargetChooser<T> thisChooser, TargetChooser<T> subChooser);

        TargetChooser<T> tag(T val);

        T unwrap(T val);
    }
}
