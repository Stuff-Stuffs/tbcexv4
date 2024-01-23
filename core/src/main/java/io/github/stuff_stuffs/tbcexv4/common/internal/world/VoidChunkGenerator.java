package io.github.stuff_stuffs.tbcexv4.common.internal.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class VoidChunkGenerator extends ChunkGenerator {
    private static final Codec<VoidChunkGenerator> CODEC = BiomeSource.CODEC.xmap(VoidChunkGenerator::new, ChunkGenerator::getBiomeSource);

    public VoidChunkGenerator(final BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    public void addStructureReferences(final StructureWorldAccess world, final StructureAccessor structureAccessor, final Chunk chunk) {
    }

    @Override
    public StructurePlacementCalculator createStructurePlacementCalculator(final RegistryWrapper<StructureSet> structureSetRegistry, final NoiseConfig noiseConfig, final long seed) {
        return StructurePlacementCalculator.create(noiseConfig, 0, biomeSource, Stream.of());
    }

    @Override
    public void generateFeatures(final StructureWorldAccess world, final Chunk chunk, final StructureAccessor structureAccessor) {
    }

    @Override
    public Pool<SpawnSettings.SpawnEntry> getEntitySpawnList(final RegistryEntry<Biome> biome, final StructureAccessor accessor, final SpawnGroup group, final BlockPos pos) {
        return Pool.empty();
    }

    @Nullable
    @Override
    public Pair<BlockPos, RegistryEntry<Structure>> locateStructure(final ServerWorld world, final RegistryEntryList<Structure> structures, final BlockPos center, final int radius, final boolean skipReferencedStructures) {
        return null;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(final ChunkRegion chunkRegion, final long seed, final NoiseConfig noiseConfig, final BiomeAccess biomeAccess, final StructureAccessor structureAccessor, final Chunk chunk, final GenerationStep.Carver carverStep) {

    }

    @Override
    public void buildSurface(final ChunkRegion region, final StructureAccessor structures, final NoiseConfig noiseConfig, final Chunk chunk) {

    }

    @Override
    public void populateEntities(final ChunkRegion region) {

    }

    @Override
    public int getWorldHeight() {
        return 384;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(final Executor executor, final Blender blender, final NoiseConfig noiseConfig, final StructureAccessor structureAccessor, final Chunk chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 64;
    }

    @Override
    public int getMinimumY() {
        return -64;
    }

    @Override
    public int getHeight(final int x, final int z, final Heightmap.Type heightmap, final HeightLimitView world, final NoiseConfig noiseConfig) {
        return -64;
    }

    @Override
    public VerticalBlockSample getColumnSample(final int x, final int z, final HeightLimitView world, final NoiseConfig noiseConfig) {
        final BlockState[] states = new BlockState[384];
        Arrays.fill(states, Blocks.AIR.getDefaultState());
        return new VerticalBlockSample(-64, states);
    }

    @Override
    public void getDebugHudText(final List<String> text, final NoiseConfig noiseConfig, final BlockPos pos) {

    }
}
