package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather.CollisionChecker;
import net.minecraft.world.BlockView;

import java.util.function.Consumer;

public class JumpNeighbourFinder implements NeighbourFinder {
    public static final String JUMP_COST = "jump_cost";
    public static final double DEFAULT_JUMP_COST = 3;

    @Override
    public Instance apply(final PatherOptions options, final BattleParticipantView participant) {
        return new InstanceImpl(participant.bounds(), participant.battleState().bounds(), participant.battleState().environment().asBlockView(), options.getValue(JUMP_COST, DEFAULT_JUMP_COST));
    }

    protected static class InstanceImpl implements Instance {
        private final CollisionChecker checker;
        private final double cost;

        private InstanceImpl(final BattleParticipantBounds bounds, final BattleBounds battleBounds, final BlockView environment, final double cost) {
            this.cost = cost;
            checker = new CollisionChecker(bounds.width(), bounds.height(), battleBounds, environment);
        }

        @Override
        public void find(final Pather.PathingNode previous, final NeighbourData data, final Consumer<Pather.PathingNode> consumer) {
            if (!previous.onFloor()) {
                return;
            }
            final int x = previous.x();
            final int y = previous.y();
            final int z = previous.z();
            if (!checker.check(x, y + 1, z, Double.NaN)) {
                return;
            }
            tryAdj(x + 1, y + 1, z, previous, data, consumer);
            tryAdj(x - 1, y + 1, z, previous, data, consumer);
            tryAdj(x, y + 1, z + 1, previous, data, consumer);
            tryAdj(x, y + 1, z - 1, previous, data, consumer);
        }

        protected void tryAdj(final int x, final int y, final int z, final Pather.PathingNode previous, final NeighbourData data, final Consumer<Pather.PathingNode> consumer) {
            if (checker.check(x, y, z, Double.NaN)) {
                final boolean floor = checker.lastFloorHeight > 0 || checker.floorHeight(x, y - 1, z) != 0;
                consumer.accept(new Pather.PathingNode(previous, previous.cost() + cost, previous.depth() + 1, Movement.JUMP, floor, x, y, z));
            }
        }
    }
}
