package io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface BattleTracerView {
    <T extends BattleTraceEvent> Node<T> byHandle(Handle<T> handle);

    Optional<Node<?>> mostRecent(Predicate<Node<?>> predicate);

    <T extends BattleTraceEvent> Optional<Node<T>> mostRecent(Predicate<Node<T>> predicate, Class<T> clazz);

    boolean contains(Timestamp timestamp);

    Node<?> byTimestamp(Timestamp timestamp);

    Stream<? extends Node<?>> all();

    <T extends BattleTraceEvent> Stream<? extends Node<T>> all(Class<T> type);

    Stream<? extends Node<?>> after(Timestamp timestamp);

    <T extends BattleTraceEvent> Stream<? extends Node<T>> after(Timestamp timestamp, Class<T> type);

    Stream<? extends Node<?>> before(Timestamp timestamp);

    <T extends BattleTraceEvent> Stream<? extends Node<T>> before(Timestamp timestamp, Class<T> type);

    Stream<? extends Node<?>> between(Timestamp start, Timestamp end);

    <T extends BattleTraceEvent> Stream<? extends Node<T>> between(Timestamp start, Timestamp end, Class<T> type);

    Node<?> latest();

    interface Handle<T extends BattleTraceEvent> {
    }

    interface Node<T extends BattleTraceEvent> {
        T event();

        Handle<T> handle();

        @Nullable Node<?> parent();

        Set<? extends Handle<?>> children();

        Timestamp timeStamp();
    }

    interface SpanView<T extends BattleTraceEvent> {
        Node<T> node();

        BattleTracerView tracer();
    }

    interface Timestamp extends Comparable<Timestamp> {
    }
}
