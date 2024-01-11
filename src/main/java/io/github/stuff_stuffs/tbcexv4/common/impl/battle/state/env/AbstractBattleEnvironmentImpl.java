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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public abstract class AbstractBattleEnvironmentImpl extends DeltaSnapshotParticipant<AbstractBattleEnvironmentImpl.Delta> implements BattleEnvironment {
    protected final Battle battle;

    protected AbstractBattleEnvironmentImpl(final Battle battle) {
        this.battle = battle;
    }

    @Override
    public boolean setBlockState(final int x, final int y, final int z, final BlockState state, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        try (final var preSpan = tracer.push(new CoreBattleTraceEvents.PreSetBlockState(x, y, z, state), transactionContext)) {
            if (x < 0 || y < 0 || z < 0 || x >= battle.xSize() || y >= battle.ySize() || z >= battle.zSize()) {
                return false;
            }
            final BlockState oldState = getBlockState0(x, y, z);
            if (oldState == state) {
                return true;
            }
            if (!battle.state().events().invoker(BasicEnvEvents.PRE_SET_BLOCK_STATE_EVENT_KEY).onPreSetBlockStateEvent(battle.state(), x, y, z, state, transactionContext, preSpan)) {
                return false;
            }
            setBlockState0(x, y, z, state);
            delta(transactionContext, new BlockDelta(x, y, z, oldState));
            try (final var span = preSpan.push(new CoreBattleTraceEvents.SetBlockState(x, y, z, oldState, state), transactionContext)) {
                battle.state().events().invoker(BasicEnvEvents.POST_SET_BLOCK_STATE_EVENT_KEY).onPostSetBlockStateEvent(battle.state(), x, y, z, oldState, transactionContext, span);
            }
            return true;
        }
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
        try (final var preSpan = tracer.push(new CoreBattleTraceEvents.PreSetBiome(x, y, z, biome), transactionContext)) {
            if (x < 0 || y < 0 || z < 0 || x >= battle.xSize() || y >= battle.ySize() || z >= battle.zSize()) {
                return false;
            }
            final RegistryEntry<Biome> current = getBiome0(x, y, z);
            if (current.equals(biome)) {
                return true;
            }
            if (!battle.state().events().invoker(BasicEnvEvents.PRE_SET_BIOME_KEY).onPreSetBiome(battle.state(), x, y, z, current, biome, transactionContext, preSpan)) {
                return false;
            }
            setBiome0(x, y, z, biome);
            delta(transactionContext, new BiomeDelta(x, y, z, current));
            try (final var span = preSpan.push(new CoreBattleTraceEvents.SetBiome(x, y, z, current, biome), transactionContext)) {
                battle.state().events().invoker(BasicEnvEvents.POST_SET_BIOME_KEY).onPostSetBiome(battle.state(), x, y, z, current, biome, transactionContext, span);
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
                return 0;
            }

            @Override
            public boolean hasBiomes() {
                return true;
            }

            @Override
            public @UnknownNullability RegistryEntry<Biome> getBiomeFabric(final BlockPos pos) {
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

    private record BlockDelta(int x, int y, int z, BlockState state) implements Delta {
        @Override
        public void apply(final AbstractBattleEnvironmentImpl environment) {
            environment.setBlockState0(x, y, z, state);
        }
    }

    private record BiomeDelta(int x, int y, int z, RegistryEntry<Biome> biome) implements Delta {
        @Override
        public void apply(final AbstractBattleEnvironmentImpl environment) {
            environment.setBiome0(x, y, z, biome);
        }
    }
}
