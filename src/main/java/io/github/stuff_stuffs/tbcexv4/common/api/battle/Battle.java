package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;

@EventViewable(viewClass = BattleView.class)
public interface Battle extends BattleView {
    @Override
    BattleState state();

    void pushAction(BattleAction action);

    TurnManager turnManager();
}
