package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PatherImpl implements Pather {
    private final NeighbourFinder[] finders;
    private final Predicate<PathingNode> endNodeValidator;

    public PatherImpl(final NeighbourFinder[] finders, final Predicate<PathingNode> validator) {
        this.finders = finders;
        endNodeValidator = validator;
    }

    @Override
    public Paths compute(final PathingNode startingNode, final PatherOptions options, final BattleParticipantView participant) {
        final NeighbourFinder.Instance[] instances = new NeighbourFinder.Instance[finders.length];
        int index = 0;
        for (final NeighbourFinder finder : finders) {
            instances[index++] = finder.apply(options, participant);
        }
        final Int2ObjectMap<PathingNode> visited = new Int2ObjectOpenHashMap<>(512);
        final Int2ObjectMap<IntSet> longerAdjacency = new Int2ObjectOpenHashMap<>(512);
        visited.put(Paths.pack(startingNode.x(), startingNode.y(), startingNode.z()), startingNode);
        final PriorityQueue<PathingNode> queue = new ObjectHeapPriorityQueue<>(512);
        queue.enqueue(startingNode);
        final NeighbourFinder.NeighbourData data = (x, y, z) -> {
            final PathingNode node = visited.get(Paths.pack(x, y, z));
            if (node == null) {
                return Double.POSITIVE_INFINITY;
            }
            return node.cost();
        };
        final Consumer<PathingNode> consumer = node -> {
            final int key = Paths.pack(node.x(), node.y(), node.z());
            final PathingNode old = visited.get(key);
            if (old == null || node.cost() < old.cost()) {
                visited.put(key, node);
                if (old != null) {
                    final PathingNode prev = old.prev();
                    if (prev != null) {
                        final int prevKey = Paths.pack(prev.x(), prev.y(), prev.z());
                        longerAdjacency.computeIfAbsent(prevKey, i -> new IntOpenHashSet(4)).add(key);
                    }
                }
                queue.enqueue(node);
            } else {
                final PathingNode prev = node.prev();
                if (prev != null) {
                    final int prevKey = Paths.pack(prev.x(), prev.y(), prev.z());
                    longerAdjacency.computeIfAbsent(prevKey, i -> new IntOpenHashSet(4)).add(key);
                }
            }
        };
        final int maxDepth = (int) options.getValue(PatherOptions.MAX_DEPTH_KEY, 32);
        while (!queue.isEmpty()) {
            final PathingNode next = queue.dequeue();
            if (next.depth() > maxDepth || visited.get(Paths.pack(next.x(), next.y(), next.z())) != next) {
                continue;
            }
            for (final NeighbourFinder.Instance instance : instances) {
                instance.find(next, data, consumer);
            }
        }
        final IntSet terminalSet = new IntOpenHashSet(visited.keySet());
        final Int2ObjectMap<PathNode> processedNodes = new Int2ObjectLinkedOpenHashMap<>(visited.size());
        for (final Int2ObjectMap.Entry<PathingNode> entry : Int2ObjectMaps.fastIterable(visited)) {
            final PathingNode node = entry.getValue();
            final PathingNode prev = node.prev();
            final boolean valid = endNodeValidator.test(node);
            if (prev != null && valid) {
                terminalSet.remove(Paths.pack(prev.x(), prev.y(), prev.z()));
            }
            if (valid) {
                convert(processedNodes, node);
            }
        }
        for (final Int2ObjectMap.Entry<IntSet> entry : Int2ObjectMaps.fastIterable(longerAdjacency)) {
            final IntIterator iterator = entry.getValue().iterator();
            boolean anyAdjValid = false;
            while (iterator.hasNext()) {
                if (processedNodes.containsKey(iterator.nextInt())) {
                    anyAdjValid = true;
                    break;
                }
            }
            if (anyAdjValid) {
                terminalSet.remove(entry.getIntKey());
            }
        }
        final IntIterator iterator = terminalSet.iterator();
        final List<PathNode> terminals = new ArrayList<>(terminalSet.size());
        while (iterator.hasNext()) {
            final int key = iterator.nextInt();
            if (endNodeValidator.test(visited.get(key))) {
                final PathNode node = processedNodes.get(key);
                terminals.add(node);
            }
        }
        return new PathsImpl(processedNodes, terminals);
    }

    private PathNode convert(Int2ObjectMap<PathNode> processedNodes, PathingNode node) {
        int key = Paths.pack(node.x(), node.y(), node.z());
        PathNode cursor = processedNodes.get(key);
        if(cursor!=null) {
            return cursor;
        }
        if(node.prev()==null) {
            PathNode pathNode = new PathNode(null, node.movement(), new BattlePos(node.x(), node.y(), node.z()));
            processedNodes.put(key, pathNode);
            return pathNode;
        }
        PathNode prev = convert(processedNodes, node.prev());
        PathNode pathNode = new PathNode(prev, node.movement(), new BattlePos(node.x(), node.y(), node.z()));
        processedNodes.put(key, pathNode);
        return pathNode;
    }

    private static final class PathsImpl implements Paths {
        private final Int2ObjectMap<PathNode> nodes;
        private final IntSet terminalKeys;
        private final List<PathNode> terminals;

        private PathsImpl(final Int2ObjectMap<PathNode> nodes, final List<PathNode> terminals) {
            this.nodes = nodes;
            this.terminals = terminals;
            terminalKeys = new IntOpenHashSet(terminals.size());
            for (final PathNode terminal : terminals) {
                BattlePos pos = terminal.pos();
                terminalKeys.add(Paths.pack(pos.x(), pos.y(), pos.z()));
            }
        }

        @Override
        public @Nullable Pather.PathNode get(final int x, final int y, final int z) {
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

        @Override
        public boolean isTerminal(final PathNode node) {
            BattlePos pos = node.pos();
            return terminalKeys.contains(Paths.pack(pos.x(), pos.y(), pos.z()));
        }
    }
}
