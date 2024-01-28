package io.github.stuff_stuffs.tbcexv4util.trace;

import io.github.stuff_stuffs.tbcexv4util.log.BattleLogContext;
import io.github.stuff_stuffs.tbcexv4util.log.BattleLogLevel;

public interface BattleTraceEvent {
    default boolean involves(final Object o) {
        return false;
    }

    default void log(final BattleTracerView tracer, final BattleTracerView.Handle<?> handle, final BattleLogContext context) {
        if (context.level() == BattleLogLevel.NONE) {
            return;
        }
        final BattleTracerView.Node<?> node = tracer.byHandle(handle);
        for (final BattleTracerView.Handle<?> child : node.children()) {
            final BattleTracerView.Node<?> childNode = tracer.byHandle(child);
            childNode.event().log(tracer, child, context);
        }
    }
}
