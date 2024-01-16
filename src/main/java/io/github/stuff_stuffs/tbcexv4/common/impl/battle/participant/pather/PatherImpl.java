package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.NeighbourFinder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.PatherOptions;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PatherImpl implements Pather {
    private final NeighbourFinder[] finders;
    private final Predicate<PathNode> endNodeValidator;

    public PatherImpl(final NeighbourFinder[] finders, final Predicate<PathNode> validator) {
        this.finders = finders;
        endNodeValidator = validator;
    }

    @Override
    public Paths compute(final PathNode startingNode, final PatherOptions options, final BattleParticipantView participant) {
        final NeighbourFinder.Instance[] instances = new NeighbourFinder.Instance[finders.length];
        int index = 0;
        for (final NeighbourFinder finder : finders) {
            instances[index++] = finder.apply(options, participant);
        }
        final Int2ObjectMap<PathNode> visited = new Int2ObjectOpenHashMap<>(512);
        visited.put(Paths.pack(startingNode.x(), startingNode.y(), startingNode.z()), startingNode);
        final PriorityQueue<PathNode> queue = new ObjectHeapPriorityQueue<>(512);
        queue.enqueue(startingNode);
        final NeighbourFinder.NeighbourData data = (x, y, z) -> {
            final PathNode node = visited.get(Paths.pack(x, y, z));
            if (node == null) {
                return Double.POSITIVE_INFINITY;
            }
            return node.cost();
        };
        final BiFunction<PathNode, PathNode, PathNode> min = (n0, n1) -> n0.cost() <= n1.cost() ? n0 : n1;
        final Consumer<PathNode> consumer = node -> {
            if (visited.merge(Paths.pack(node.x(), node.y(), node.z()), node, min) == node) {
                queue.enqueue(node);
            }
        };
        final int maxDepth = (int) options.getValue(PatherOptions.MAX_DEPTH_KEY, 32);
        while (!queue.isEmpty()) {
            final PathNode next = queue.dequeue();
            if (next.depth() > maxDepth || visited.get(Paths.pack(next.x(), next.y(), next.z())) != next) {
                continue;
            }
            for (final NeighbourFinder.Instance instance : instances) {
                instance.find(next, data, consumer);
            }
        }
        final IntSet terminalSet = new IntOpenHashSet(visited.keySet());
        final Int2ObjectMap<PathNode> processedNodes = new Int2ObjectLinkedOpenHashMap<>(visited.size());
        for (final Int2ObjectMap.Entry<PathNode> entry : visited.int2ObjectEntrySet()) {
            final PathNode node = entry.getValue();
            final PathNode prev = node.prev();
            final boolean valid = endNodeValidator.test(node);
            if (prev != null && valid) {
                terminalSet.remove(Paths.pack(prev.x(), prev.y(), prev.z()));
            }
            if (valid) {
                processedNodes.put(entry.getIntKey(), entry.getValue());
            }
        }
        final IntIterator iterator = terminalSet.iterator();
        final List<PathNode> terminals = new ArrayList<>(terminalSet.size());
        while (iterator.hasNext()) {
            final int key = iterator.nextInt();
            final PathNode node = visited.get(key);
            if (endNodeValidator.test(node)) {
                terminals.add(node);
            }
        }
        return new PathsImpl(processedNodes, terminals);
    }


    private static final class PathsImpl implements Paths {
        private final Int2ObjectMap<PathNode> nodes;
        private final List<PathNode> terminals;

        private PathsImpl(final Int2ObjectMap<PathNode> nodes, final List<PathNode> terminals) {
            this.nodes = nodes;
            this.terminals = terminals;
        }

        @Override
        public @Nullable PathNode get(final int x, final int y, final int z) {
            final int key = Paths.pack(x, y, z);
            return nodes.get(key);
        }

        @Override
        public boolean canCache() {
            return true;
        }

        @Override
        public Stream<? extends PathNode> terminal() {
            return terminals.stream();
        }

        @Override
        public Stream<? extends PathNode> all() {
            return nodes.values().stream();
        }
    }
}
