package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;

import java.util.Set;
import java.util.function.Function;

public class PrefixPlan<T extends Target> implements Plan {
    private final TargetChooser<T> chooser;
    private final Function<T, Plan> factory;

    public PrefixPlan(final TargetChooser<T> chooser, final Function<T, Plan> factory) {
        this.chooser = chooser;
        this.factory = factory;
    }

    @Override
    public Set<TargetType<?>> targetTypes() {
        return Set.of(chooser.type());
    }

    @Override
    public <T0 extends Target> TargetChooser<T0> ofType(final TargetType<T0> type) {
        if (type == chooser.type()) {
            return (TargetChooser<T0>) chooser;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Plan addTarget(final Target target) {
        return factory.apply((T) target);
    }

    @Override
    public boolean canBuild() {
        return false;
    }

    @Override
    public BattleActionRequest build() {
        throw new UnsupportedOperationException();
    }
}
