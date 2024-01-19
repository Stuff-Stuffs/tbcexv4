package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class BattleRenderStateImpl extends RenderStateImpl implements BattleRenderState {
    private final Map<BattleParticipantHandle, ParticipantRenderStateImpl> participantStates = new Object2ReferenceOpenHashMap<>();

    @Override
    public void update(final double time) {
        super.update(time);
        for (final ParticipantRenderStateImpl state : participantStates.values()) {
            state.update(time);
        }
    }

    @Override
    public ParticipantRenderState getParticipant(final BattleParticipantHandle handle) {
        return participantStates.get(handle);
    }

    @Override
    public Set<BattleParticipantHandle> participants() {
        return Collections.unmodifiableSet(participantStates.keySet());
    }

    @Override
    public void addParticipant(final BattleParticipantHandle handle) {
        participantStates.putIfAbsent(handle, new ParticipantRenderStateImpl());
    }

    @Override
    public void removeParticipant(final BattleParticipantHandle handle) {
        participantStates.remove(handle);
    }

    @Override
    public void setTime(final double time) {
        update(time);
    }
}
