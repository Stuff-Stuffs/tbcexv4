package io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.env.BasicEnvEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public abstract class AbstractBattleEnvironmentImpl extends DeltaSnapshotParticipant<AbstractBattleEnvironmentImpl.Delta> implements BattleEnvironment {
    protected final Battle battle;

    protected AbstractBattleEnvironmentImpl(final Battle battle) {
        this.battle = battle;
    }

    @Override
    public boolean setBlockState(final int x, final int y, final int z, final BlockState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        if (x < 0 || y < 0 || z < 0 || x > battle.xSize() || y > battle.ySize() || z > battle.zSize()) {
            return false;
        }
        final BlockState oldState = getBlockState0(x, y, z);
        if (oldState == state) {
            return true;
        }
        if (!battle.state().events().invoker(BasicEnvEvents.PRE_SET_BLOCK_STATE_EVENT_KEY).onPreSetBlockStateEvent(battle.state(), x, y, z, state, transactionContext, tracer)) {
            return false;
        }
        setBlockState0(x, y, z, state);
        delta(transactionContext, new BlockDelta(x, y, z, oldState));
        try (final var span = tracer.push(new CoreBattleTraceEvents.SetBlockState(x, y, z, oldState, state), transactionContext)) {
            battle.state().events().invoker(BasicEnvEvents.POST_SET_BLOCK_STATE_EVENT_KEY).onPostSetBlockStateEvent(battle.state(), x, y, z, oldState, transactionContext, span);
        }
        return true;
    }

    @Override
    public BlockState blockState(final int x, final int y, final int z) {
        if (x < 0 || y < 0 || z < 0 || x > battle.xSize() || y > battle.ySize() || z > battle.zSize()) {
            return Blocks.AIR.getDefaultState();
        }
        return getBlockState0(x, y, z);
    }

    @Override
    protected void revertDelta(final Delta delta) {
        delta.apply(this);
    }

    protected abstract void setBlockState0(int x, int y, int z, BlockState state);

    protected abstract BlockState getBlockState0(int x, int y, int z);

    public sealed interface Delta {
        void apply(AbstractBattleEnvironmentImpl environment);
    }

    private record BlockDelta(int x, int y, int z, BlockState state) implements Delta {
        @Override
        public void apply(final AbstractBattleEnvironmentImpl environment) {
            environment.setBlockState0(x, y, z, state);
        }
    }
}
