package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface Pather<M> {
    PathCache<M> compute(PatherOptions options, BattleParticipantView participant);

    interface PathCache<M> {
        int BITS = Integer.bitCount(BattlePos.MAX);
        int SHIFT = BITS;
        int MASK = (1 << BITS) - 1;

        @Nullable PathNode<M> get(int x, int y, int z);

        Stream<? extends PathNode<M>> terminal();

        Stream<? extends PathNode<M>> all();

        static int pack(final int x, final int y, final int z) {
            return (x & MASK) << 2 * SHIFT | (y & MASK) << SHIFT | z & MASK;
        }

        static int unpackX(final int packed) {
            return (packed >>> 2 * SHIFT) & MASK;
        }

        static int unpackY(final int packed) {
            return (packed >>> SHIFT) & MASK;
        }

        static int unpackZ(final int packed) {
            return packed & MASK;
        }
    }

    interface PathNode<M> {
        @Nullable PathNode<M> previous();

        double cost();

        M movement();

        int x();

        int y();

        int z();
    }
}
