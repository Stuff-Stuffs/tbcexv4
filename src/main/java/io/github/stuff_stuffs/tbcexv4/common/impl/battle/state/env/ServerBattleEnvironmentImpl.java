package io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.WorldChunk;

public class ServerBattleEnvironmentImpl extends AbstractBattleEnvironmentImpl {
    private final BlockPos.Mutable scratch;

    public ServerBattleEnvironmentImpl(final Battle battle) {
        super(battle);
        scratch = new BlockPos.Mutable();
    }

    public BattleView battle() {
        return battle;
    }

    @Override
    protected void setBlockState0(final int x, final int y, final int z, final BlockState state) {
        ((BattleStateImpl) battle.state()).ensureBattleOngoing();
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        ((ServerBattleImpl) battle).world().runAction(world -> world.setBlockState(scratch, state));
    }

    @Override
    protected BlockState getBlockState0(final int x, final int y, final int z) {
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        return ((ServerBattleImpl) battle).world().getBlockState(scratch);
    }

    @Override
    protected void setBiome0(final int x, final int y, final int z, final RegistryEntry<Biome> biome) {
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        final int biomeX = BiomeCoords.fromBlock(scratch.getX());
        final int biomeY = BiomeCoords.fromBlock(scratch.getY());
        final int biomeZ = BiomeCoords.fromBlock(scratch.getZ());
        final ServerBattleWorld world = ((ServerBattleImpl) battle).world();
        final WorldChunk chunk = world.getChunk(scratch.getX() >> 4, scratch.getZ() >> 4);
        chunk.populateBiomes((x1, y1, z1, noise) -> {
            if (x1 == biomeX && y1 == biomeY && z1 == biomeZ) {
                return biome;
            }
            return chunk.getBiomeForNoiseGen(x1, y1, z1);
        }, null);
        Tbcexv4.updateBiomes(world.getRegistryKey(), chunk);
    }

    @Override
    protected RegistryEntry<Biome> getBiome0(final int x, final int y, final int z) {
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        return ((ServerBattleImpl) battle).world().getChunk(scratch).getBiomeForNoiseGen(BiomeCoords.fromBlock(scratch.getX()), BiomeCoords.fromBlock(scratch.getY()), BiomeCoords.fromBlock(scratch.getZ()));
    }
}
