package io.github.stuff_stuffs.tbcexv4.common.impl.battle.tracer;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracerView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Stream;

public class BattleTracerImpl extends DeltaSnapshotParticipant<BattleTracerImpl.Delta> implements BattleTracer {
    private final SortedMap<HandleImpl<?>, NodeImpl<?>> nodes;
    private final List<NodeImpl<?>> flattened;
    private final NodeImpl<?> rootNode;
    private long nextTimestamp = Long.MIN_VALUE + 1;

    public <T extends BattleTraceEvent> BattleTracerImpl(final T root) {
        nodes = new Object2ObjectAVLTreeMap<>(HandleImpl.COMPARATOR);
        flattened = new ObjectArrayList<>();
        final TimestampImpl first = new TimestampImpl(this, nextTimestamp++);
        final HandleImpl<T> handle = new HandleImpl<>(this, first);
        final NodeImpl<?> node = new NodeImpl<>(root, handle, null);
        nodes.put(handle, node);
        rootNode = node;
    }

    private void verify(final TimestampImpl timestamp) {
        if (timestamp.parent != this) {
            throw new RuntimeException();
        }
    }

    @Override
    public <T extends BattleTraceEvent> Node<T> byHandle(final Handle<T> handle) {
        final HandleImpl<T> casted = (HandleImpl<T>) handle;
        if (casted.parent != this) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return (Node<T>) nodes.get(casted);
    }

    @Override
    public boolean contains(final Timestamp timestamp) {
        final TimestampImpl casted = (TimestampImpl) timestamp;
        if (casted.parent != this) {
            throw new IllegalArgumentException();
        }
        final HandleImpl<?> handle = new HandleImpl<>(this, casted);
        return nodes.containsKey(handle);
    }

    @Override
    public Node<?> byTimestamp(final Timestamp timestamp) {
        final TimestampImpl casted = (TimestampImpl) timestamp;
        if (casted.parent != this) {
            throw new IllegalArgumentException();
        }
        final HandleImpl<?> handle = new HandleImpl<>(this, casted);
        final NodeImpl<?> node = nodes.get(handle);
        if (node == null) {
            throw new IllegalArgumentException();
        }
        return node;
    }

    @Override
    public <T extends BattleTraceEvent> Span<T> push(final T event, final BattleTransactionContext transactionContext) {
        return push(event, rootNode, transactionContext);
    }

    private <T extends BattleTraceEvent> Span<T> push(final T event, final NodeImpl<?> parent, final BattleTransactionContext transactionContext) {
        final TimestampImpl timestamp = new TimestampImpl(this, nextTimestamp++);
        final HandleImpl<T> handle = new HandleImpl<>(this, timestamp);
        delta(transactionContext, new Delta(handle));
        final NodeImpl<T> node = new NodeImpl<>(event, handle, parent);
        nodes.put(handle, node);
        parent.children.add(handle);
        flattened.clear();
        return new SpanImpl<>(node, this);
    }

    private void flatten() {
        if (flattened.size() != nodes.size()) {
            flattened.clear();
            flattened.addAll(nodes.values());
        }
    }

    @Override
    public Stream<? extends Node<?>> all() {
        flatten();
        return flattened.stream().sorted();
    }

    @Override
    public <T extends BattleTraceEvent> Stream<? extends Node<T>> all(final Class<T> type) {
        flatten();
        //noinspection unchecked
        return flattened.stream().filter(node -> type.isInstance(node.event)).map(node -> (NodeImpl<T>) node);
    }

    private int find(final Timestamp timestamp) {
        flatten();
        verify((TimestampImpl) timestamp);
        int lower = 0;
        int upper = flattened.size();
        while (lower < upper) {
            final int mid = (lower + upper) >> 1;
            final NodeImpl<?> node = flattened.get(mid);
            final int compare = node.handle.timestamp.compareTo(timestamp);
            if (compare == 0) {
                return mid;
            } else if (compare < 0) {
                lower = mid;
            } else {
                upper = mid;
            }
        }
        return -1;
    }

    @Override
    public Stream<? extends Node<?>> after(final Timestamp timestamp) {
        flatten();
        final int i = find(timestamp);
        if (i < 0) {
            throw new RuntimeException();
        }
        if (i + 1 == flattened.size()) {
            return Stream.of();
        }
        return flattened.subList(i + 1, flattened.size()).stream();
    }

    @Override
    public <T extends BattleTraceEvent> Stream<? extends Node<T>> after(final Timestamp timestamp, final Class<T> type) {
        flatten();
        final int i = find(timestamp);
        if (i < 0) {
            throw new RuntimeException();
        }
        if (i + 1 == flattened.size()) {
            return Stream.of();
        }
        //noinspection unchecked
        return flattened.subList(i + 1, flattened.size()).stream().filter(node -> type.isInstance(node.event)).map(node -> (NodeImpl<T>) node);
    }

    @Override
    public Stream<? extends Node<?>> before(final Timestamp timestamp) {
        flatten();
        final int i = find(timestamp);
        if (i < 0) {
            throw new RuntimeException();
        }
        if (i == 0) {
            return Stream.of();
        }
        return flattened.subList(0, i).stream();
    }

    @Override
    public <T extends BattleTraceEvent> Stream<? extends Node<T>> before(final Timestamp timestamp, final Class<T> type) {
        flatten();
        final int i = find(timestamp);
        if (i < 0) {
            throw new RuntimeException();
        }
        if (i == 0) {
            return Stream.of();
        }
        //noinspection unchecked
        return flattened.subList(0, i).stream().filter(node -> type.isInstance(node.event)).map(node -> (NodeImpl<T>) node);
    }

    @Override
    public Stream<? extends Node<?>> between(final Timestamp start, final Timestamp end) {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("Start must happen before or be equal to end!");
        }
        flatten();
        final int lower = find(start);
        if (lower < 0) {
            throw new RuntimeException();
        }
        final int upper = find(end);
        if (upper < 0 || lower > upper) {
            throw new RuntimeException();
        }
        if (lower == upper) {
            return Stream.of();
        }
        return flattened.subList(lower + 1, upper).stream();
    }

    @Override
    public <T extends BattleTraceEvent> Stream<? extends Node<T>> between(final Timestamp start, final Timestamp end, final Class<T> type) {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("Start must happen before or be equal to end!");
        }
        flatten();
        final int lower = find(start);
        if (lower < 0) {
            throw new RuntimeException();
        }
        final int upper = find(end);
        if (upper < 0 || lower > upper) {
            throw new RuntimeException();
        }
        if (lower == upper) {
            return Stream.of();
        }
        //noinspection unchecked
        return flattened.subList(lower + 1, upper).stream().filter(node -> type.isInstance(node.event)).map(node -> (NodeImpl<T>) node);
    }

    @Override
    public Node<?> latest() {
        flatten();
        if (flattened.isEmpty()) {
            throw new RuntimeException();
        }
        return flattened.get(flattened.size() - 1);
    }

    @Override
    protected void revertDelta(final Delta delta) {
        nodes.remove(delta.handle);
        flattened.clear();
    }

    private record HandleImpl<T extends BattleTraceEvent>(
            BattleTracerImpl parent,
            TimestampImpl timestamp
    ) implements Handle<T> {
        public static final Comparator<HandleImpl<?>> COMPARATOR = Comparator.comparing(HandleImpl::timestamp);
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

        @Override
        public Timestamp timeStamp() {
            return handle.timestamp;
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

    private record TimestampImpl(BattleTracerImpl parent, long id) implements Timestamp {
        @Override
        public int compareTo(@NotNull final BattleTracerView.Timestamp o) {
            if (!(o instanceof TimestampImpl timestamp)) {
                throw new RuntimeException();
            }
            if (parent != timestamp.parent) {
                throw new RuntimeException("Comparing timestamps from different tracers is forbidden!");
            }
            return Long.compare(id, timestamp.id);
        }
    }

    public record Delta(HandleImpl<?> handle) {
    }
}
