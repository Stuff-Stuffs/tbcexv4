package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.Target;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetChooser;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetType;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class PrefixPlan<T extends Target> implements Plan {
    private final TargetChooser<T> chooser;
    private final Function<T, Plan> factory;
    private final PlanType type;

    public PrefixPlan(final TargetChooser<T> chooser, final Function<T, Plan> factory, PlanType type) {
        this.chooser = chooser;
        this.factory = factory;
        this.type = type;
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
    public List<BattleAction> build() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PlanType type() {
        return type;
    }
}
