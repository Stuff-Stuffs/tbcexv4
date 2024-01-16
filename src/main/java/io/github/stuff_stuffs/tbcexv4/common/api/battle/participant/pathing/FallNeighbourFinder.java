package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather.CollisionChecker;
import net.minecraft.world.BlockView;

import java.util.function.Consumer;

public class FallNeighbourFinder implements NeighbourFinder {
    public static final String FALL_COST_SMALL = "fall_cost_small";
    public static final String FALL_COST_BIG = "fall_cost_big";
    public static final String FALL_COST_SIZE = "fall_cost_big_size";
    public static final double DEFAULT_FALL_COST_SMALL = 1;
    public static final double DEFAULT_FALL_COST_BIG = 8;
    public static final double DEFAULT_FALL_COST_BIG_SIZE = 4;

    @Override
    public Instance apply(final PatherOptions options, final BattleParticipantView participant) {
        return new InstanceImpl(participant.bounds(), participant.battleState().bounds(), participant.battleState().environment().asBlockView(), options.getValue(FALL_COST_SMALL, DEFAULT_FALL_COST_SMALL), options.getValue(FALL_COST_BIG, DEFAULT_FALL_COST_BIG), options.getValue(FALL_COST_SIZE, DEFAULT_FALL_COST_BIG_SIZE));
    }

    protected static class InstanceImpl implements Instance {
        private final CollisionChecker checker;
        private final double smallCost;
        private final double bigCost;
        private final double bigSize;

        private InstanceImpl(final BattleParticipantBounds bounds, final BattleBounds battleBounds, final BlockView environment, final double smallCost, final double bigCost, final double bigSize) {
            this.smallCost = smallCost;
            this.bigCost = bigCost;
            this.bigSize = bigSize;
            checker = new CollisionChecker(bounds.width(), bounds.height(), battleBounds, environment);
        }

        @Override
        public void find(final Pather.PathNode previous, final NeighbourData data, final Consumer<Pather.PathNode> consumer) {
            if (previous.onFloor()) {
                return;
            }
            final int x = previous.x();
            final int y = previous.y();
            final int z = previous.z();
            if (checker.check(x, y - 1, z, Double.NaN)) {
                final boolean floor = checker.lastFloorHeight > 0 || checker.floorHeight(x, y - 2, z) > 0.0;
                int fallDistance = 0;
                Pather.PathNode cursor = previous;
                while (cursor != null && cursor.movement() == Movement.FALL) {
                    cursor = cursor.prev();
                    fallDistance++;
                }
                final double cost = fallDistance >= bigSize ? bigCost : smallCost;
                consumer.accept(new Pather.PathNode(previous, previous.cost() + cost, previous.depth() + 1, Movement.FALL, floor, x, y - 1, z));
            }
        }
    }
}
