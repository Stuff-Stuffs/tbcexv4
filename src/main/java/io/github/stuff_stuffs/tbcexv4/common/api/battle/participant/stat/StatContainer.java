package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;

@EventViewable(viewClass = StatContainerView.class)
public interface StatContainer extends StatContainerView {
    <T> ModifierHandle addStateModifier(Stat<T> stat, Modifier<T> modifier, StatModificationPhase phase, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    interface ModificationContext {
    }

    interface Modifier<T> {
        T compute(T currentValue, ModificationContext context);
    }

    interface ModifierHandle {
        boolean alive();

        void kill();
    }
}
