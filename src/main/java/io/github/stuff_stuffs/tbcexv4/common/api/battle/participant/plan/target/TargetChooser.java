package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;

public interface TargetChooser<T extends Target> {
    TargetType<T> type();

    Iterator<? extends T> all();

    T choose(double temperature, Random random, BattleTransactionContext context);

    double weight(T target, double temperature, Random random, BattleTransactionContext context);

    double weight();
}
