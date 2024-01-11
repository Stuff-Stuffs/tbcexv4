package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;

import java.util.Set;

public interface Plan {
    Set<TargetType<?>> targetTypes();

    <T extends Target> TargetChooser<T> ofType(TargetType<T> type);

    Plan addTarget(Target target);

    boolean canBuild();

    BattleActionRequest build();
}
