package io.github.stuff_stuffs.tbcexv4.common.impl.battle.tracer;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class BattleTracerImpl extends DeltaSnapshotParticipant<BattleTracerImpl.Delta> implements BattleTracer {
    private final List<NodeImpl<?>> nodes;

    public BattleTracerImpl() {
        nodes = new ArrayList<>();
    }

    @Override
    public <T extends BattleTraceEvent> Node<T> byHandle(final Handle<T> handle) {
        final HandleImpl<T> casted = (HandleImpl<T>) handle;
        if (casted.parent != this) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return (Node<T>) nodes.get(casted.id);
    }

    @Override
    public <T extends BattleTraceEvent> Span<T> push(final T event, final BattleTransactionContext transactionContext) {
        return push(event, null, transactionContext);
    }

    private <T extends BattleTraceEvent> Span<T> push(final T event, final @Nullable NodeImpl<?> parent, final BattleTransactionContext transactionContext) {
        final int id = nodes.size();
        delta(transactionContext, new Delta(id));
        final HandleImpl<T> handle = new HandleImpl<>(id, this);
        final NodeImpl<T> node = new NodeImpl<>(event, handle, parent);
        nodes.add(node);
        if (parent != null) {
            parent.children.add(handle);
        }
        return new SpanImpl<>(node, this);
    }

    @Override
    public Stream<? extends Node<?>> eventStream() {
        return nodes.stream().unordered();
    }

    @Override
    public <T extends BattleTraceEvent> Stream<? extends Node<T>> eventStream(final Class<T> type) {
        //noinspection unchecked
        return nodes.stream().filter(node -> type.isInstance(node.event)).map(node -> (NodeImpl<T>) node);
    }

    @Override
    protected void revertDelta(final Delta delta) {
        nodes.set(delta.id, null);
    }

    private record HandleImpl<T extends BattleTraceEvent>(int id, BattleTracerImpl parent) implements Handle<T> {
    }

    private static final class NodeImpl<T extends BattleTraceEvent> implements Node<T> {
        private final T event;
        private final HandleImpl<T> handle;
        private final @Nullable NodeImpl<?> parent;
        private final Set<HandleImpl<?>> children;

        private NodeImpl(final T event, final HandleImpl<T> handle, @Nullable final NodeImpl<?> parent) {
            this.event = event;
            this.handle = handle;
            this.parent = parent;
            children = new ObjectOpenHashSet<>();
        }

        @Override
        public T event() {
            return event;
        }

        @Override
        public Handle<T> handle() {
            return handle;
        }

        @Override
        public @Nullable Node<?> parent() {
            return parent;
        }

        @Override
        public Set<HandleImpl<?>> children() {
            return children;
        }
    }

    private static final class SpanImpl<T extends BattleTraceEvent> implements Span<T> {
        private final NodeImpl<T> node;
        private final BattleTracerImpl tracer;
        private boolean open;

        private SpanImpl(final NodeImpl<T> node, final BattleTracerImpl tracer) {
            this.node = node;
            this.tracer = tracer;
            open = true;
        }

        @Override
        public <T0 extends BattleTraceEvent> Span<T0> push(final T0 event, final BattleTransactionContext transactionContext) {
            if (!open) {
                throw new RuntimeException();
            }
            return tracer.push(event, node, transactionContext);
        }

        @Override
        public Node<T> node() {
            return node;
        }

        @Override
        public BattleTracer tracer() {
            return tracer;
        }

        @Override
        public void close() {
            open = false;
        }
    }

    public record Delta(int id) {
    }
}
