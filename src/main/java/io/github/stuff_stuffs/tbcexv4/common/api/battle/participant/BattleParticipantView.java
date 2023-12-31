package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatContainerView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMapView;

public interface BattleParticipantView {
    BattleParticipantPhase phase();

    EventMapView events();

    BattleStateView battleState();

    BattleParticipantBounds bounds();

    BattlePos pos();

    BattleParticipantTeam team();

    BattleParticipantHandle handle();

    StatContainerView stats();

    double health();
}
