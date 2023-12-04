package io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Stream;

public interface BattleTracerView {
    <T extends BattleTraceEvent> Node<T> byHandle(Handle<T> handle);

    Stream<? extends Node<?>> eventStream();

    <T extends BattleTraceEvent> Stream<? extends Node<T>> eventStream(Class<T> type);

    interface Handle<T extends BattleTraceEvent> {
    }

    interface Node<T extends BattleTraceEvent> {
        T event();

        Handle<T> handle();

        @Nullable Node<?> parent();

        Set<? extends Handle<?>> children();
    }

    interface SpanView<T extends BattleTraceEvent> {
        Node<T> node();

        BattleTracerView tracer();
    }
}
