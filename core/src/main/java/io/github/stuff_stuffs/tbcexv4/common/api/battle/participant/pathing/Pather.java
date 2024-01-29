package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather.PatherImpl;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Pather {
    Paths compute(PathingNode startingNode, PatherOptions options, BattleParticipantView participant);

    interface Paths {
        int BITS = Integer.bitCount(BattlePos.MAX);
        int SHIFT = BITS;
        int MASK = (1 << BITS) - 1;

        @Nullable Pather.PathNode get(int x, int y, int z);

        boolean canCache();

        boolean adjacent(int x0, int y0, int z0, int x1, int y1, int z1);

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

    record PathNode(@Nullable PathNode prev, Movement movement, BattlePos pos) {
        public static final Codec<PathNode> CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<PathNode, T>> decode(final DynamicOps<T> ops, final T input) {
                final Optional<MapLike<T>> opt = ops.getMap(input).result();
                if (opt.isEmpty()) {
                    return DataResult.error(() -> "Expected a map!");
                }
                final MapLike<T> mapLike = opt.get();
                final Optional<Number> optSize = ops.getNumberValue(mapLike.get("size")).result();
                if (optSize.isEmpty()) {
                    return DataResult.error(() -> "Not a number");
                }
                final int size = optSize.get().intValue();
                if (size <= 0) {
                    return DataResult.error(() -> "Size must be positive!");
                }
                final List<Movement> movements = new ArrayList<>(size);
                final Optional<Consumer<Consumer<T>>> optMovement = ops.getList(mapLike.get("movement")).result();
                if (optMovement.isEmpty()) {
                    return DataResult.error(() -> "Not a list!");
                }
                optMovement.get().accept(mov -> {
                    final Optional<Movement> decoded = Movement.CODEC.parse(ops, mov).result();
                    decoded.ifPresent(movements::add);
                });
                if (movements.size() != size) {
                    return DataResult.error(() -> "Error during movement decode!");
                }
                final List<BattlePos> positions = new ArrayList<>(size);
                final Optional<Consumer<Consumer<T>>> optPosition = ops.getList(mapLike.get("position")).result();
                if (optPosition.isEmpty()) {
                    return DataResult.error(() -> "Not a list!");
                }
                optPosition.get().accept(mov -> {
                    final Optional<BattlePos> decoded = BattlePos.CODEC.parse(ops, mov).result();
                    decoded.ifPresent(positions::add);
                });
                if (positions.size() != size) {
                    return DataResult.error(() -> "Error during position decode!");
                }
                PathNode prev = null;
                for (int i = size - 1; i >= 0; i--) {
                    final PathNode next = new PathNode(prev, movements.get(i), positions.get(i));
                    prev = next;
                }
                return DataResult.success(Pair.of(prev, ops.empty()));
            }

            @Override
            public <T> DataResult<T> encode(PathNode input, final DynamicOps<T> ops, final T prefix) {
                final List<T> movements = new ArrayList<>();
                final List<T> positions = new ArrayList<>();
                while (input != null) {
                    {
                        final DataResult<T> encoded = Movement.CODEC.encode(input.movement, ops, prefix);
                        final Optional<T> result = encoded.result();
                        if (result.isPresent()) {
                            movements.add(result.get());
                        } else {
                            return DataResult.error(() -> "Error during encoding movement!");
                        }
                    }
                    {
                        final DataResult<T> encoded = BattlePos.CODEC.encode(input.pos, ops, prefix);
                        final Optional<T> result = encoded.result();
                        if (result.isPresent()) {
                            positions.add(result.get());
                        } else {
                            return DataResult.error(() -> "Error during encoding movement!");
                        }
                    }
                    input = input.prev;
                }
                return ops.mapBuilder().add("size", ops.createInt(movements.size())).add("movement", ops.createList(movements.stream())).add("position", ops.createList(positions.stream())).build(prefix);
            }
        };
    }

    record PathingNode(
            @Nullable Pather.PathingNode prev,
            double cost,
            int depth,
            Movement movement,
            boolean onFloor,
            int x,
            int y,
            int z
    ) implements Comparable<PathingNode> {

        public PathingNode(final Optional<PathingNode> prev, final double cost, final int depth, final Movement movement, final boolean onFloor, final int x, final int y, final int z) {
            this(prev.orElse(null), cost, depth, movement, onFloor, x, y, z);
        }

        @Override
        public int compareTo(final PathingNode o) {
            return Double.compare(cost, o.cost);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final PathingNode node)) {
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

    static Pather create(final NeighbourFinder[] finders, final Predicate<PathingNode> validator) {
        return new PatherImpl(finders, validator);
    }
}
