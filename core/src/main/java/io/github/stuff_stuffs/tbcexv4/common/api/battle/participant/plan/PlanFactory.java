package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;

import java.util.function.Consumer;

public interface PlanFactory {
    void create(BattleParticipantView participant, Consumer<Plan> consumer);
}
