package io.github.stuff_stuffs.tbcexv4.common.internal.world;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;

public class BattlePersistentState extends PersistentState {
    public static final Type<BattlePersistentState> TYPE = new Type<>(BattlePersistentState::new, BattlePersistentState::new, null);
    private final ObjectAVLTreeSet<Range> openRanges;
    private final ObjectAVLTreeSet<Range> closedRanges;

    public BattlePersistentState() {
        this(new ObjectAVLTreeSet<>(Range.COMPARATOR), new ObjectAVLTreeSet<>(Range.COMPARATOR));
        openRanges.add(new Range(-100_000, 100_000));
    }

    public BattlePersistentState(final ObjectAVLTreeSet<Range> openRanges, final ObjectAVLTreeSet<Range> closedRanges) {
        this.openRanges = openRanges;
        this.closedRanges = closedRanges;
    }

    public BattlePersistentState(final NbtCompound nbt) {
        final int[] open = nbt.getIntArray("open");
        openRanges = new ObjectAVLTreeSet<>(Range.COMPARATOR);
        for (int i = 0; i < open.length / 2; i++) {
            openRanges.add(new Range(open[i * 2], open[i * 2 + 1]));
        }
        final int[] closed = nbt.getIntArray("closed");
        closedRanges = new ObjectAVLTreeSet<>(Range.COMPARATOR);
        for (int i = 0; i < closed.length / 2; i++) {
            closedRanges.add(new Range(closed[i * 2], closed[i * 2 + 1]));
        }
    }

    public void deallocate(final BlockPos pos, final int size) {
        final Range range = new Range(pos.getX(), pos.getX() + size);
        if (closedRanges.remove(range)) {
            openRanges.add(range);
            merge(range);
        }
    }

    private void merge(Range range) {
        while (true) {
            final SortedSet<Range> headSet = openRanges.headSet(range);
            if (!headSet.isEmpty()) {
                final Range last = headSet.last();
                if (last.end >= range.start) {
                    openRanges.remove(last);
                    range = new Range(last.start, Math.max(last.end, range.end));
                    openRanges.add(range);
                    continue;
                }
            }
            break;
        }
        while (true) {
            final SortedSet<Range> tailSet = openRanges.tailSet(range);
            if (tailSet.size() > 1) {
                final Iterator<Range> iterator = tailSet.iterator();
                iterator.next();
                final Range next = iterator.next();
                if (next.start <= range.end) {
                    openRanges.remove(next);
                    range = new Range(next.start, range.end);
                    continue;
                }
            }
            break;
        }
    }

    public Optional<BlockPos> allocate(int size, final ServerBattleWorld world) {
        size = (size + 15) & ~15;
        final Iterator<Range> iterator = openRanges.iterator();
        while (iterator.hasNext()) {
            final Range next = iterator.next();
            final int s = next.end - next.start;
            if (s >= size) {
                iterator.remove();
                final int start = next.start;
                final int end = start + size;
                if (s != size) {
                    openRanges.add(new Range(end, next.end));
                }
                return Optional.of(new BlockPos(start, world.getBottomY(), 0));
            }
        }
        return Optional.empty();
    }

    @Override
    public NbtCompound writeNbt(final NbtCompound nbt) {
        final int[] open = new int[openRanges.size() * 2];
        int i = 0;
        for (final Range range : openRanges) {
            open[i * 2] = range.start;
            open[i * 2 + 1] = range.end;
            i++;
        }
        final int[] closed = new int[closedRanges.size() * 2];
        i = 0;
        for (final Range range : closedRanges) {
            closed[i * 2] = range.start;
            closed[i * 2 + 1] = range.end;
            i++;
        }
        nbt.putIntArray("open", open);
        nbt.putIntArray("closed", closed);
        return nbt;
    }

    private record Range(int start, int end) {
        private static final Comparator<Range> COMPARATOR = Comparator.comparingInt(Range::start);
    }
}
