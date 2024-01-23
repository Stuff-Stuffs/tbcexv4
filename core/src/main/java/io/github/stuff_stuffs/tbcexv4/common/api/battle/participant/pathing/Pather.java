package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.util.NullSafeOptionalFieldCodec;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather.PatherImpl;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Pather {
    Paths compute(PathNode startingNode, PatherOptions options, BattleParticipantView participant);

    interface Paths {
        int BITS = Integer.bitCount(BattlePos.MAX);
        int SHIFT = BITS;
        int MASK = (1 << BITS) - 1;

        @Nullable PathNode get(int x, int y, int z);

        boolean canCache();

        Stream<? extends PathNode> terminal();

        Stream<? extends PathNode> all();

        boolean isTerminal(PathNode node);

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

    record PathNode(
            @Nullable PathNode prev,
            double cost,
            int depth,
            Movement movement,
            boolean onFloor,
            int x,
            int y,
            int z
    ) implements Comparable<PathNode> {
        public static final Codec<PathNode> CODEC = Codecs.createRecursive("PathNode", codec -> RecordCodecBuilder.create(instance -> instance.group(
                new NullSafeOptionalFieldCodec<>("prev", codec).forGetter(node -> Optional.ofNullable(node.prev)),
                Codec.DOUBLE.fieldOf("cost").forGetter(node -> node.cost),
                Codec.INT.fieldOf("depth").forGetter(node -> node.depth),
                Movement.CODEC.fieldOf("movement").forGetter(node -> node.movement),
                Codec.BOOL.fieldOf("onFloor").forGetter(node -> node.onFloor),
                Codec.INT.fieldOf("x").forGetter(node -> node.x),
                Codec.INT.fieldOf("y").forGetter(node -> node.y),
                Codec.INT.fieldOf("z").forGetter(node -> node.z)
        ).apply(instance, PathNode::new)));

        public PathNode(final Optional<PathNode> prev, final double cost, final int depth, final Movement movement, final boolean onFloor, final int x, final int y, final int z) {
            this(prev.orElse(null), cost, depth, movement, onFloor, x, y, z);
        }

        @Override
        public int compareTo(final PathNode o) {
            return Double.compare(cost, o.cost);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final PathNode node)) {
                return false;
            }

            if (Double.compare(cost, node.cost) != 0) {
                return false;
            }
            if (depth != node.depth) {
                return false;
            }
            if (onFloor != node.onFloor) {
                return false;
            }
            if (x != node.x) {
                return false;
            }
            if (y != node.y) {
                return false;
            }
            if (z != node.z) {
                return false;
            }
            return movement == node.movement;
        }

        @Override
        public int hashCode() {
            int result;
            final long temp;
            temp = Double.doubleToLongBits(cost);
            result = (int) (temp ^ (temp >>> 32));
            result = 31 * result + depth;
            result = 31 * result + movement.hashCode();
            result = 31 * result + (onFloor ? 1 : 0);
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }

    static Pather create(final NeighbourFinder[] finders, final Predicate<PathNode> validator) {
        return new PatherImpl(finders, validator);
    }
}