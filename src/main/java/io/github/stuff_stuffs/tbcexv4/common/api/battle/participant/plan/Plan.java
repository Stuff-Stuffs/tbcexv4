package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;

import java.util.List;
import java.util.Set;

public interface Plan {
    Set<TargetType<?>> targetTypes();

    <T extends Target> TargetChooser<T> ofType(TargetType<T> type);

    Plan addTarget(Target target);

    boolean canBuild();

    List<BattleAction> build();

    PlanType type();
}
