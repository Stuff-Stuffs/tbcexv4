package io.github.stuff_stuffs.tbcexv4.common.api.ai;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracerView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;

import java.util.Optional;
import java.util.Set;

public final class Scorers {
    public static Scorer health(final BattleParticipantHandle handle) {
        return (state, tracer) -> {
            final BattleParticipantView participant = state.participant(handle);
            if (participant == null) {
                return 0;
            }
            final double revPercent = 1 - participant.health() / participant.stats().get(Tbcexv4Registries.Stats.MAX_HEALTH);
            return 1 - (revPercent * revPercent);
        };
    }

    public static Scorer teamHealth(final BattleParticipantTeam team) {
        return (state, tracer) -> {
            final Set<BattleParticipantHandle> participants = state.participants(team);
            if (participants.isEmpty()) {
                return 0;
            }
            double sum = 0;
            for (final BattleParticipantHandle h : participants) {
                final BattleParticipantView participant = state.participant(h);
                final double revPercent = 1 - participant.health() / participant.stats().get(Tbcexv4Registries.Stats.MAX_HEALTH);
                sum = sum + 1 - (revPercent * revPercent);
            }
            return sum / participants.size();
        };
    }

    public static Scorer teamHealth(final BattleParticipantHandle handle) {
        return (state, tracer) -> {
            final BattleParticipantView exemplar = state.participant(handle);
            final BattleParticipantTeam team;
            if (exemplar == null) {
                final Optional<BattleTracerView.Node<CoreBattleTraceEvents.ParticipantSetTeam>> teamNode = tracer.mostRecent(node -> node.event().handle().equals(handle), CoreBattleTraceEvents.ParticipantSetTeam.class);
                if (teamNode.isEmpty()) {
                    return 0;
                }
                team = teamNode.get().event().newTeam();
            } else {
                team = exemplar.team();
            }
            final Set<BattleParticipantHandle> participants = state.participants(team);
            double sum = 0;
            for (final BattleParticipantHandle h : participants) {
                final BattleParticipantView participant = state.participant(h);
                final double revPercent = 1 - participant.health() / participant.stats().get(Tbcexv4Registries.Stats.MAX_HEALTH);
                sum = sum + 1 - (revPercent * revPercent);
            }
            return sum / participants.size();
        };
    }

    private Scorers() {
    }
}
