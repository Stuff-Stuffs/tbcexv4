package io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.env.BasicEnvEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.PreSetBiomeTrace;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.PreSetBlockStateTrace;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.SetBiomeTrace;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.SetBlockStateTrace;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.BattleParticipantImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.pather.CollisionChecker;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class AbstractBattleEnvironmentImpl extends DeltaSnapshotParticipant<AbstractBattleEnvironmentImpl.Delta> implements BattleEnvironment {
    protected final Battle battle;
    private @Nullable Map<BattleParticipantHandle, Pather.Paths> cached;

    protected AbstractBattleEnvironmentImpl(final Battle battle) {
        this.battle = battle;
    }

    @Override
    public boolean setBlockState(final int x, final int y, final int z, final BlockState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        if (x < 0 || y < 0 || z < 0 || x >= battle.xSize() || y >= battle.ySize() || z >= battle.zSize()) {
            return false;
        }
        final BlockState oldState = getBlockState0(x, y, z);
        if (oldState == state) {
            return true;
        }
        final BattlePos pos = new BattlePos(x, y, z);
        try (final var preSpan = tracer.push(new PreSetBlockStateTrace(pos, state), transactionContext)) {
            final BattleState battleState = battle.state();
            if (!battleState.events().invoker(BasicEnvEvents.PRE_SET_BLOCK_STATE_EVENT_KEY, transactionContext).onPreSetBlockStateEvent(battleState, x, y, z, state, transactionContext, preSpan)) {
                return false;
            }
            setBlockState0(x, y, z, state);
            for (final BattleParticipantHandle handle : battleState.participants()) {
                final CollisionChecker checker = ((BattleParticipantImpl) battleState.participant(handle)).collisionChecker();
                if (!checker.check(x, y, z, Double.NaN)) {
                    setBlockState0(x, y, z, oldState);
                    return false;
                }
            }
            delta(transactionContext, new BlockDelta(x, y, z, oldState, cached));
            cached = null;
            try (final var span = preSpan.push(new SetBlockStateTrace(pos, oldState, state), transactionContext)) {
                battleState.events().invoker(BasicEnvEvents.POST_SET_BLOCK_STATE_EVENT_KEY, transactionContext).onPostSetBlockStateEvent(battleState, x, y, z, oldState, transactionContext, span);
            }
            return true;
        }
    }

    @Override
    public void clearCachePath(final BattleTransactionContext context) {
        delta(context, new PathClearDelta(cached));
        cached = null;
    }

    @Override
    public BlockState blockState(final int x, final int y, final int z) {
        if (x < 0 || y < 0 || z < 0 || x >= battle.xSize() || y >= battle.ySize() || z >= battle.zSize()) {
            return Blocks.AIR.getDefaultState();
        }
        return getBlockState0(x, y, z);
    }

    @Override
    public RegistryEntry<Biome> biome(final int x, final int y, final int z) {
        return getBiome0(Math.max(Math.min(x, battle.xSize() - 1), 0), Math.max(Math.min(y, battle.ySize() - 1), 0), Math.max(Math.min(z, battle.zSize() - 1), 0));
    }

    @Override
    public boolean setBiome(final int x, final int y, final int z, final RegistryEntry<Biome> biome, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        if (x < 0 || y < 0 || z < 0 || x >= battle.xSize() || y >= battle.ySize() || z >= battle.zSize()) {
            return false;
        }
        final RegistryEntry<Biome> current = getBiome0(x, y, z);
        if (current.equals(biome)) {
            return true;
        }
        final BattlePos pos = new BattlePos(x, y, z);
        try (final var preSpan = tracer.push(new PreSetBiomeTrace(pos, biome), transactionContext)) {
            if (!battle.state().events().invoker(BasicEnvEvents.PRE_SET_BIOME_KEY, transactionContext).onPreSetBiome(battle.state(), x, y, z, current, biome, transactionContext, preSpan)) {
                return false;
            }
            setBiome0(x, y, z, biome);
            delta(transactionContext, new BiomeDelta(x, y, z, current));
            try (final var span = preSpan.push(new SetBiomeTrace(pos, current, biome), transactionContext)) {
                battle.state().events().invoker(BasicEnvEvents.POST_SET_BIOME_KEY, transactionContext).onPostSetBiome(battle.state(), x, y, z, current, biome, transactionContext, span);
            }
            return true;
        }
    }

    @Override
    public BlockView asBlockView() {
        return new BlockView() {
            @Nullable
            @Override
            public BlockEntity getBlockEntity(final BlockPos pos) {
                return null;
            }

            @Override
            public BlockState getBlockState(final BlockPos pos) {
                return blockState(pos.getX(), pos.getY(), pos.getZ());
            }

            @Override
            public FluidState getFluidState(final BlockPos pos) {
                return getBlockState(pos).getFluidState();
            }

            @Override
            public int getHeight() {
                return battle.ySize();
            }

            @Override
            public int getBottomY() {
                return battle.worldY(0);
            }

            @Override
            public boolean hasBiomes() {
                return true;
            }

            @Override
            public RegistryEntry<Biome> getBiomeFabric(final BlockPos pos) {
                return biome(pos.getX(), pos.getY(), pos.getZ());
            }
        };
    }

    @Override
    protected void revertDelta(final Delta delta) {
        delta.apply(this);
    }

    protected abstract void setBlockState0(int x, int y, int z, BlockState state);

    protected abstract BlockState getBlockState0(int x, int y, int z);

    protected abstract void setBiome0(int x, int y, int z, RegistryEntry<Biome> biome);

    protected abstract RegistryEntry<Biome> getBiome0(int x, int y, int z);

    public sealed interface Delta {
        void apply(AbstractBattleEnvironmentImpl environment);
    }

    private record BlockDelta(int x, int y, int z, BlockState state,
                              @Nullable Map<BattleParticipantHandle, Pather.Paths> cachedPaths) implements Delta {
        @Override
        public void apply(final AbstractBattleEnvironmentImpl environment) {
            environment.setBlockState0(x, y, z, state);
            environment.cached = cachedPaths;
        }
    }

    private record BiomeDelta(int x, int y, int z, RegistryEntry<Biome> biome) implements Delta {
        @Override
        public void apply(final AbstractBattleEnvironmentImpl environment) {
            environment.setBiome0(x, y, z, biome);
        }
    }

    private record PathClearDelta(@Nullable Map<BattleParticipantHandle, Pather.Paths> cachedPaths) implements Delta {
        @Override
        public void apply(final AbstractBattleEnvironmentImpl environment) {
            environment.cached = cachedPaths;
        }
    }
}
