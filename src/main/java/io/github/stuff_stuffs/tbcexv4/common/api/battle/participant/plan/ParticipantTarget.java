package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;

public record ParticipantTarget(BattleParticipantHandle participant) implements Target {
}
