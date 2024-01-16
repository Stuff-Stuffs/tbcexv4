package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.util.math.random.Random;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

public final class TargetChoosers {
    public static Optional<TargetChooser<ParticipantTarget>> helpParticipant(final BattleParticipantView current, final Predicate<BattleParticipantHandle> predicate, final double weight) {
        final AbstractParticipantTargetChooser chooser = new AbstractParticipantTargetChooser(current.battleState(), predicate, weight) {
            @Override
            protected double weight0(final BattleParticipantHandle obj, final double temperature, final Random random, final BattleTransactionContext context) {
                final BattleParticipantTeam team = current.team();
                final BattleParticipantTeam otherTeam = state.participant(obj).team();
                final BattleParticipantTeamRelation relation = state.relation(team, otherTeam);
                if (relation == BattleParticipantTeamRelation.HOSTILE) {
                    return 0;
                } else if (relation == BattleParticipantTeamRelation.NEUTRAL) {
                    return 1;
                }
                return 100;
            }
        };
        if (chooser.iterator().hasNext()) {
            return Optional.of(chooser);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<TargetChooser<ParticipantTarget>> hurtParticipant(final BattleParticipantView current, final Predicate<BattleParticipantHandle> predicate, final double weight) {
        final AbstractParticipantTargetChooser chooser = new AbstractParticipantTargetChooser(current.battleState(), predicate, weight) {
            @Override
            protected double weight0(final BattleParticipantHandle obj, final double temperature, final Random random, final BattleTransactionContext context) {
                final BattleParticipantTeam team = current.team();
                final BattleParticipantTeam otherTeam = state.participant(obj).team();
                if (state.relation(team, otherTeam) == BattleParticipantTeamRelation.HOSTILE) {
                    return 100;
                }
                return 0;
            }
        };
        if (chooser.iterator().hasNext()) {
            return Optional.of(chooser);
        } else {
            return Optional.empty();
        }
    }

    public static abstract class AbstractParticipantTargetChooser extends AbstractTargetChooser<BattleParticipantHandle, ParticipantTarget> {
        private final Predicate<BattleParticipantHandle> predicate;
        private final double weight;

        protected AbstractParticipantTargetChooser(final BattleStateView state, final Predicate<BattleParticipantHandle> predicate, final double weight) {
            super(state);
            this.predicate = predicate;
            this.weight = weight;
        }

        @Override
        public TargetType<ParticipantTarget> type() {
            return Tbcexv4Registries.TargetTypes.PARTICIPANT_TARGET;
        }

        @Override
        public double weight() {
            return weight;
        }

        @Override
        protected BattleParticipantHandle extract(final ParticipantTarget target) {
            return target.participant();
        }

        @Override
        protected Iterator<? extends BattleParticipantHandle> iterator() {
            return state.participants().stream().filter(predicate).iterator();
        }

        @Override
        protected ParticipantTarget create(final BattleParticipantHandle obj) {
            return new ParticipantTarget(obj);
        }
    }

    private TargetChoosers() {
    }
}
