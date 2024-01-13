package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.PatherOptions;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironmentView;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractPatherImpl<M, N extends AbstractPatherImpl.Node<M>> implements Pather<M> {
    @Override
    public PathCache<M> compute(final PatherOptions options, final BattleParticipantView participant) {
        final BattleEnvironmentView environment = participant.battleState().environment();
        final BattleParticipantBounds bounds = participant.bounds();
        final CollisionChecker checker = new CollisionChecker(bounds.width(), bounds.height(), participant.battleState().bounds(), environment.asBlockView());
        final N start = start(participant, checker, options);
        if (start == null) {
            return new PathCacheImpl<>(Int2ObjectMaps.emptyMap(), ObjectLists.emptyList());
        }
        final PriorityQueue<N> heap = new ObjectHeapPriorityQueue<>(Node.COMPARATOR);
        final Int2ObjectMap<N> byKey = new Int2ObjectLinkedOpenHashMap<>();
        byKey.put(PathCache.pack(start.x, start.y, start.z), start);
        final NeighbourData<M, N> neighbourData = new NeighbourData<>() {
            @Override
            public double cost(final int x, final int y, final int z) {
                if (!checker.inBounds(x, y, z)) {
                    return Double.NaN;
                }
                final N node = byKey.get(PathCache.pack(x, y, z));
                return node == null ? Double.POSITIVE_INFINITY : node.cost;
            }

            @Override
            public void add(final N node) {
                if (node.cost < cost(node.x, node.y, node.z)) {
                    byKey.put(PathCache.pack(node.x, node.y, node.z), node);
                    heap.enqueue(node);
                }
            }
        };
        final int maxDepth = (int) options.getValue(PatherOptions.MAX_DEPTH_KEY, 32);
        while (!heap.isEmpty()) {
            final N current = heap.dequeue();
            if (current.depth > maxDepth) {
                continue;
            }
            appendNeighbours(current, neighbourData, participant, checker, options);
        }
        final IntSet terminal = new IntOpenHashSet(byKey.keySet());
        final Int2ObjectMap<PathNodeImpl<M>> nodes = new Int2ObjectLinkedOpenHashMap<>(byKey.size(), Hash.DEFAULT_LOAD_FACTOR);
        for (final N value : byKey.values()) {
            final PathNodeImpl<M> node = create(value, participant, options);
            final int key = PathCache.pack(value.x, value.y, value.z);
            nodes.put(key, node);
            terminal.remove(key);
        }
        final List<PathNodeImpl<M>> terminals = new ArrayList<>(terminal.size());
        final IntIterator iterator = terminal.iterator();
        while (iterator.hasNext()) {
            terminals.add(nodes.get(iterator.nextInt()));
        }
        return new PathCacheImpl<>(nodes, terminals);
    }

    protected abstract PathNodeImpl<M> create(N node, BattleParticipantView participant, PatherOptions options);

    protected abstract @Nullable N start(BattleParticipantView participant, CollisionChecker checker, PatherOptions options);

    protected abstract void appendNeighbours(N current, NeighbourData<M, N> neighbourData, BattleParticipantView participant, CollisionChecker checker, PatherOptions options);

    protected static final class PathCacheImpl<M> implements PathCache<M> {
        private final Int2ObjectMap<PathNodeImpl<M>> nodes;
        private final List<PathNodeImpl<M>> terminal;

        private PathCacheImpl(final Int2ObjectMap<PathNodeImpl<M>> nodes, final List<PathNodeImpl<M>> terminal) {
            this.nodes = nodes;
            this.terminal = terminal;
        }

        @Override
        public @Nullable PathNode<M> get(final int x, final int y, final int z) {
            final int key = PathCache.pack(x, y, z);
            return nodes.get(key);
        }

        @Override
        public Stream<? extends PathNode<M>> terminal() {
            return terminal.stream();
        }

        @Override
        public Stream<? extends PathNode<M>> all() {
            return nodes.values().stream();
        }
    }

    protected static class PathNodeImpl<M> implements PathNode<M> {
        private final @Nullable PathNodeImpl<M> prev;
        protected final double cost;
        private final M movement;
        private final int x;
        private final int y;
        private final int z;

        protected PathNodeImpl(@Nullable final PathNodeImpl<M> prev, final double cost, final M movement, final int x, final int y, final int z) {
            this.prev = prev;
            this.cost = cost;
            this.movement = movement;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public @Nullable PathNode<M> previous() {
            return prev;
        }

        @Override
        public double cost() {
            return cost;
        }

        @Override
        public M movement() {
            return movement;
        }

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int z() {
            return z;
        }
    }

    protected interface NeighbourData<M, N extends Node<M>> {
        double cost(int x, int y, int z);

        void add(N node);
    }

    protected static class Node<M> {
        private static final Comparator<Node<?>> COMPARATOR = Comparator.comparingDouble(node -> node.cost);
        public final int x, y, z;
        public final double floorHeight;
        public final double cost;
        public final M movement;
        public final Node<M> prev;
        public final int depth;

        protected Node(final int x, final int y, final int z, final double floorHeight, final double cost, final M movement, final Node<M> prev, final int depth) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.floorHeight = floorHeight;
            this.cost = cost;
            this.movement = movement;
            this.prev = prev;
            this.depth = depth;
        }
    }
}
