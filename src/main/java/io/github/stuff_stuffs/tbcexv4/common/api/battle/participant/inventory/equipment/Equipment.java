package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

@EventViewable(viewClass = EquipmentView.class)
public interface Equipment extends EquipmentView {
    void init(BattleParticipant participant, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

    void deinit(BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);
}
