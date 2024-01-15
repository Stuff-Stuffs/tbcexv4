package io.github.stuff_stuffs.tbcexv4.common.api.battle.action;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;

public record ActionSource(BattleParticipantHandle actor, int energy) {
}
