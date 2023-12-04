package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;

import java.util.Optional;
import java.util.UUID;

public interface BattleParticipantInitialState {
    BattleParticipantBounds bounds();

    BattlePos pos();

    BattleParticipantTeam team();

    default Optional<UUID> id() {
        return Optional.empty();
    }

    default void initialize(final BattleState state, final BattleParticipant participant) {
    }
}
