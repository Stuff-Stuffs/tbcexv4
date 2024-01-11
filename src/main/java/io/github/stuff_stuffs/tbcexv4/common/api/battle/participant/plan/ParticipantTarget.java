package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;

public class ParticipantTarget implements Target {
    private final BattleParticipantHandle target;
    private final TargetChooser<ParticipantTarget> parent;

    public ParticipantTarget(final BattleParticipantHandle target, final TargetChooser<ParticipantTarget> parent) {
        this.target = target;
        this.parent = parent;
    }

    public BattleParticipantHandle target() {
        return target;
    }

    @Override
    public TargetChooser<ParticipantTarget> parent() {
        return parent;
    }
}
