package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;

public interface TargetChooser<T extends Target> {
    Plan parent();

    TargetType<T> type();

    Iterator<? extends T> all();

    T choose(double temperature, Random random, BattleTransactionContext context);

    double weight(T target, double temperature, Random random, BattleTransactionContext context);

    double weight();
}
