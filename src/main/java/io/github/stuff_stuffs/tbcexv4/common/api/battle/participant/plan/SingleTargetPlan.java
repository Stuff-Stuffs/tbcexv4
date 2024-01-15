package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class SingleTargetPlan<T extends Target> implements Plan {
    private final @Nullable TargetChooser<T> chooser;
    private final @Nullable T target;
    private final Function<T, List<BattleAction>> factory;
    private final PlanType type;

    public static <T extends Target> SingleTargetPlan<T> ofSingle(final TargetChooser<T> chooser, final Function<T, BattleAction> factory, final PlanType type) {
        return new SingleTargetPlan<>(chooser, factory.andThen(List::of), type);
    }

    public SingleTargetPlan(final TargetChooser<T> chooser, final Function<T, List<BattleAction>> factory, final PlanType type) {
        this.chooser = chooser;
        this.factory = factory;
        this.type = type;
        target = null;
    }

    private SingleTargetPlan(final T target, final Function<T, List<BattleAction>> factory, final PlanType type) {
        this.factory = factory;
        this.type = type;
        chooser = null;
        this.target = target;
    }

    @Override
    public Set<TargetType<?>> targetTypes() {
        if (chooser == null) {
            return Set.of();
        }
        return Set.of(chooser.type());
    }

    @Override
    public <T0 extends Target> TargetChooser<T0> ofType(final TargetType<T0> type) {
        if (chooser != null && type == chooser.type()) {
            return (TargetChooser<T0>) chooser;
        }
        throw new IllegalStateException();
    }

    @Override
    public Plan addTarget(final Target target) {
        if (canBuild()) {
            throw new IllegalStateException();
        }
        return new SingleTargetPlan<>((T) target, factory, type);
    }

    @Override
    public boolean canBuild() {
        return target != null;
    }

    @Override
    public List<BattleAction> build() {
        if (!canBuild()) {
            throw new IllegalStateException();
        }
        return factory.apply(target);
    }

    @Override
    public PlanType type() {
        return type;
    }
}
