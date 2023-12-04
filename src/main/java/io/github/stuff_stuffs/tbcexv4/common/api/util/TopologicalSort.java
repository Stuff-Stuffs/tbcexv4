package io.github.stuff_stuffs.tbcexv4.common.api.util;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TopologicalSort {
    public static <T> List<T> tieBreakingSort(final List<T> items, final ChildPredicate<T> childPredicate, final TieBreaker<T> tieBreaker) {
        if (items.isEmpty()) {
            return List.of();
        }
        final IntPriorityQueue queue = new IntHeapPriorityQueue((i0, i1) -> tieBreaker.compare(i0, i1, items));
        return sort(items, childPredicate, queue, dependencyCount(items, childPredicate));
    }

    public static <T> List<T> sort(final List<T> items, final ChildPredicate<T> childPredicate) {
        if (items.isEmpty()) {
            return List.of();
        }
        return sort(items, childPredicate, new IntArrayFIFOQueue(), dependencyCount(items, childPredicate));
    }

    private static <T> int[] dependencyCount(final List<T> items, final ChildPredicate<T> childPredicate) {
        final Set<T> set = new ObjectOpenHashSet<>();
        final int size = items.size();
        final int[] indexToDependencyCount = new int[size];
        for (int i = 0; i < size; i++) {
            final T item = items.get(i);
            if (!set.add(item)) {
                throw new IllegalStateException("Duplicate items detected!");
            }
            for (int j = 0; j < size; j++) {
                if (childPredicate.isChild(i, j, items)) {
                    indexToDependencyCount[j] += 1;
                }
            }
        }
        return indexToDependencyCount;
    }

    private static <T> List<T> sort(final List<T> items, final ChildPredicate<T> childPredicate, final IntPriorityQueue queue, final int[] indexToDependencyCount) {
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            if (indexToDependencyCount[i] == 0) {
                queue.enqueue(i);
            }
        }
        final List<T> output = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            if (queue.isEmpty()) {
                throw new IllegalStateException("Cycle detected!");
            }
            final int idx = queue.dequeueInt();
            final T item = items.get(idx);
            output.add(item);
            for (int j = 0; j < size; j++) {
                if (childPredicate.isChild(idx, j, items)) {
                    if ((--indexToDependencyCount[j]) == 0) {
                        queue.enqueue(j);
                    }
                }
            }
        }
        return output;
    }

    public interface ChildPredicate<T> {
        boolean isChild(int parent, int child, List<T> items);
    }

    public interface TieBreaker<T> {
        int compare(int first, int second, List<T> items);
    }

    private TopologicalSort() {
    }
}
