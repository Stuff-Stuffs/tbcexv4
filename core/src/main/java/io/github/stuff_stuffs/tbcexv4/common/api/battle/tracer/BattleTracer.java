package io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4util.trace.BattleTraceEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.tracer.BattleTracerImpl;
import io.github.stuff_stuffs.tbcexv4util.trace.BattleTracerView;

@EventViewable(viewClass = BattleTracerView.class)
public interface BattleTracer extends BattleTracerView {
    <T extends BattleTraceEvent> Span<T> push(T event, BattleTransactionContext transactionContext);

    @EventViewable(viewClass = BattleTracerView.SpanView.class)
    interface Span<T extends BattleTraceEvent> extends BattleTracerView.SpanView<T>, AutoCloseable {
        <T0 extends BattleTraceEvent> Span<T0> push(T0 event, BattleTransactionContext transactionContext);

        @Override
        BattleTracer tracer();

        @Override
        void close();
    }

    static BattleTracer create(final BattleTraceEvent rootEvent) {
        return new BattleTracerImpl(rootEvent);
    }
}
