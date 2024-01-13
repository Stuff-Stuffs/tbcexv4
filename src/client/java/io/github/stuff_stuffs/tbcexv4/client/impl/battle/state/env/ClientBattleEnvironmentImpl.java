package io.github.stuff_stuffs.tbcexv4.client.impl.battle.state.env;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env.AbstractBattleEnvironmentImpl;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.PalettedContainer;

public class ClientBattleEnvironmentImpl extends AbstractBattleEnvironmentImpl {
    private final int sectionX;
    private final int sectionY;
    private final int sectionZ;
    private final Section[] sections;

    public ClientBattleEnvironmentImpl(final Battle battle, final World world, final int sectionX, final int sectionY, final int sectionZ) {
        super(battle);
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
        sections = new Section[sectionX * sectionY * sectionZ];
        final Registry<Biome> registry = world.getRegistryManager().get(RegistryKeys.BIOME);
        for (int i = 0; i < sectionX; i++) {
            for (int j = 0; j < sectionY; j++) {
                for (int k = 0; k < sectionZ; k++) {
                    sections[(j * sectionZ + k) * sectionX + i] = new Section(registry);
                }
            }
        }
    }

    @Override
    protected void setBlockState0(final int x, final int y, final int z, final BlockState state) {
        final int sx = ChunkSectionPos.getSectionCoord(x);
        final int sy = ChunkSectionPos.getSectionCoord(y);
        final int sz = ChunkSectionPos.getSectionCoord(z);
        final Section section = sections[(sy * sectionZ + sz) * sectionX + sx];
        section.blockStateContainer.swapUnsafe(x & 15, y & 15, z & 15, state);
    }

    @Override
    protected BlockState getBlockState0(final int x, final int y, final int z) {
        final int sx = ChunkSectionPos.getSectionCoord(x);
        final int sy = ChunkSectionPos.getSectionCoord(y);
        final int sz = ChunkSectionPos.getSectionCoord(z);
        final Section section = sections[(sy * sectionZ + sz) * sectionX + sx];
        return section.blockStateContainer.get(x & 15, y & 15, z & 15);
    }

    @Override
    protected void setBiome0(final int x, final int y, final int z, final RegistryEntry<Biome> biome) {
        final int sx = ChunkSectionPos.getSectionCoord(x);
        final int sy = ChunkSectionPos.getSectionCoord(y);
        final int sz = ChunkSectionPos.getSectionCoord(z);
        final Section section = sections[(sy * sectionZ + sz) * sectionX + sx];
        section.biomeContainer.set(BiomeCoords.fromBlock(x) & 3, BiomeCoords.fromBlock(y) & 3, BiomeCoords.fromBlock(z) & 3, biome);
    }

    @Override
    protected RegistryEntry<Biome> getBiome0(final int x, final int y, final int z) {
        final int sx = ChunkSectionPos.getSectionCoord(x);
        final int sy = ChunkSectionPos.getSectionCoord(y);
        final int sz = ChunkSectionPos.getSectionCoord(z);
        final Section section = sections[(sy * sectionZ + sz) * sectionX + sx];
        return section.biomeContainer.get(BiomeCoords.fromBlock(x) & 3, BiomeCoords.fromBlock(y) & 3, BiomeCoords.fromBlock(z) & 3);
    }

    public Section get(final int x, final int y, final int z) {
        if (x < 0 || x >= sectionX || y < 0 || y >= sectionY || z < 0 || z >= sectionZ) {
            throw new RuntimeException();
        }
        return sections[(y * sectionZ + z) * sectionX + x];
    }

    public static final class Section {
        public PalettedContainer<BlockState> blockStateContainer;
        public PalettedContainer<RegistryEntry<Biome>> biomeContainer;

        public Section(final Registry<Biome> biomeRegistry) {
            blockStateContainer = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);
            biomeContainer = new PalettedContainer<>(biomeRegistry.getIndexedEntries(), biomeRegistry.entryOf(BiomeKeys.PLAINS), PalettedContainer.PaletteProvider.BIOME);
        }
    }
}
