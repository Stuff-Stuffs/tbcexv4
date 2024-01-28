package io.github.stuff_stuffs.tbcexv4.common.api.ai;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4util.trace.BattleTracerView;

import java.util.function.DoubleUnaryOperator;

public interface Scorer {
    double score(BattleStateView state, BattleTracerView tracer);

    default Scorer remap(final DoubleUnaryOperator op) {
        return (state, tracer) -> op.applyAsDouble(score(state, tracer));
    }

    static Scorer sum(final Scorer... scorers) {
        return (state, tracer) -> {
            double s = 0;
            for (final Scorer scorer : scorers) {
                s = s + scorer.score(state, tracer);
            }
            return s;
        };
    }

    static Scorer product(final Scorer... scorers) {
        return (state, tracer) -> {
            double s = 0;
            for (final Scorer scorer : scorers) {
                s = s * scorer.score(state, tracer);
            }
            return s;
        };
    }

    static Scorer avg(final Scorer... scorers) {
        return (state, tracer) -> {
            double s = 0;
            for (final Scorer scorer : scorers) {
                s = s + scorer.score(state, tracer);
            }
            return s / scorers.length;
        };
    }
}
