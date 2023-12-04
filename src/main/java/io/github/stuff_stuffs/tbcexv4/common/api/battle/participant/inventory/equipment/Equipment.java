package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;

public interface Equipment {
    void init(BattleParticipant participant, BattleTracer.Span<?> trace);

    void deinit(BattleTracer.Span<?> trace);
}
