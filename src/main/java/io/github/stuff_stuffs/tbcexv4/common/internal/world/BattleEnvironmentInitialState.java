package io.github.stuff_stuffs.tbcexv4.common.internal.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BattleEnvironmentInitialState {
    private final ChunkSection[] sections;
    private final int width;
    private final int height;
    private final int depth;

    public static BattleEnvironmentInitialState of(final ServerWorld world, final ChunkSectionPos lower, final ChunkSectionPos upper) {
        final int minX = Math.min(lower.getSectionX(), upper.getSectionX());
        final int minY = Math.min(lower.getSectionY(), upper.getSectionY());
        final int minZ = Math.min(lower.getSectionZ(), upper.getSectionZ());

        final int maxX = Math.max(lower.getSectionX(), upper.getSectionX());
        final int maxY = Math.max(lower.getSectionY(), upper.getSectionY());
        final int maxZ = Math.max(lower.getSectionZ(), upper.getSectionZ());

        final int width = maxX - minX;
        final int height = maxY - minY;
        final int depth = maxZ - minZ;
        final ChunkSection[] sections = new ChunkSection[width * height * depth];
        for (int i = 0; i < width; i++) {
            for (int k = 0; k < depth; k++) {
                final WorldChunk chunk = world.getChunk(i + minX, k + minZ);
                for (int j = 0; j < height; j++) {
                    final int index = i + width * (j + height * k);
                    final Registry<Biome> registry = world.getRegistryManager().get(RegistryKeys.BIOME);
                    final LightingProvider lightingProvider = world.getLightingProvider();
                    final ChunkSectionPos sectionPos = ChunkSectionPos.from(i + minX, j + minY, k + minZ);
                    if (j + minY < world.getBottomSectionCoord() || j + minY >= world.getTopSectionCoord()) {
                        sections[index] = new ChunkSection(null, sectionPos, lightingProvider, registry);
                    } else {
                        final int sectionIndex = world.sectionCoordToIndex(j + minY);
                        sections[index] = new ChunkSection(chunk.getSection(sectionIndex), sectionPos, lightingProvider, registry);
                    }
                }
            }
        }
        return new BattleEnvironmentInitialState(sections, width, height, depth);
    }

    private BattleEnvironmentInitialState(final ChunkSection[] sections, final int width, final int height, final int depth) {
        if (sections.length != width * height * depth) {
            throw new RuntimeException();
        }
        this.sections = sections;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public void apply(final ServerWorld world, final int baseSectionX, final int baseSectionZ) {
        for (int i = 0; i < width; i++) {
            for (int k = 0; k < depth; k++) {
                final WorldChunk chunk = world.getChunk(baseSectionX + i, baseSectionZ + k);
                for (int j = 0; j < height; j++) {
                    final ChunkSection section = sections[i + width * (j + height * k)];
                    ((ChunkSectionExtensions) chunk.getSection(j)).tbcexv4$copy(section.blockStates, section.biomes);
                    final ChunkSectionPos pos = ChunkSectionPos.from(baseSectionX + i, j, baseSectionZ + k);
                    world.getLightingProvider().enqueueSectionData(LightType.BLOCK, pos, section.blockLight.copy());
                    world.getLightingProvider().enqueueSectionData(LightType.SKY, pos, section.skyLight.copy());
                }
            }
        }
    }

    private static final class ChunkSection {
        private static final Codec<PalettedContainer<BlockState>> CODEC = PalettedContainer.createPalettedContainerCodec(
                Block.STATE_IDS, BlockState.CODEC, PalettedContainer.PaletteProvider.BLOCK_STATE, Blocks.AIR.getDefaultState()
        );
        private final PalettedContainer<BlockState> blockStates;
        private final PalettedContainer<RegistryEntry<Biome>> biomes;
        private final ChunkNibbleArray skyLight;
        private final ChunkNibbleArray blockLight;

        private ChunkSection(final net.minecraft.world.chunk.ChunkSection section, final ChunkSectionPos pos, final LightingProvider provider, final Registry<Biome> biomeRegistry) {
            if (section != null) {
                blockStates = section.getBlockStateContainer().copy();
                biomes = ((PalettedContainer<RegistryEntry<Biome>>) section.getBiomeContainer()).copy();
                final ChunkNibbleArray skyLightSection = provider.get(LightType.SKY).getLightSection(pos);
                skyLight = skyLightSection != null ? skyLightSection.copy() : new ChunkNibbleArray(15);
                final ChunkNibbleArray blockLightSection = provider.get(LightType.BLOCK).getLightSection(pos);
                blockLight = blockLightSection != null ? blockLightSection.copy() : new ChunkNibbleArray(15);
            } else {
                blockStates = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PalettedContainer.PaletteProvider.BLOCK_STATE);
                biomes = new PalettedContainer<>(biomeRegistry.getIndexedEntries(), biomeRegistry.entryOf(BiomeKeys.PLAINS), PalettedContainer.PaletteProvider.BIOME);
                skyLight = new ChunkNibbleArray(15);
                blockLight = new ChunkNibbleArray(15);
            }
        }

        private ChunkSection(final PalettedContainer<BlockState> blockStates, final PalettedContainer<RegistryEntry<Biome>> biomes, final ChunkNibbleArray skyLight, final ChunkNibbleArray blockLight) {
            this.blockStates = blockStates;
            this.biomes = biomes;
            this.skyLight = skyLight;
            this.blockLight = blockLight;
        }

        private NbtCompound serialize(final Registry<Biome> biomeRegistry) {
            final NbtCompound nbt = new NbtCompound();
            nbt.put("blocks", CODEC.encodeStart(NbtOps.INSTANCE, blockStates).getOrThrow(false, Tbcexv4.LOGGER::error));
            nbt.put("biomes", createBiomeCodec(biomeRegistry).encodeStart(NbtOps.INSTANCE, biomes).getOrThrow(false, Tbcexv4.LOGGER::error));
            nbt.putByteArray("skyLight", skyLight.asByteArray());
            nbt.putByteArray("blockLight", blockLight.asByteArray());
            return nbt;
        }

        private static DataResult<ChunkSection> deserialize(final NbtCompound nbt, final Registry<Biome> biomeRegistry) {
            final DataResult<PalettedContainer<BlockState>> blockRes = CODEC.parse(NbtOps.INSTANCE, nbt.get("blocks"));
            return blockRes.flatMap(blockContainer -> {
                final DataResult<PalettedContainer<RegistryEntry<Biome>>> biomesRes = createBiomeCodec(biomeRegistry).parse(NbtOps.INSTANCE, nbt.get("biomes"));
                return biomesRes.flatMap(biomeContainer -> DataResult.success(
                        new ChunkSection(
                                blockContainer,
                                biomeContainer,
                                new ChunkNibbleArray(nbt.getByteArray("skyLight")),
                                new ChunkNibbleArray(nbt.getByteArray("blockLight"))
                        )
                ));
            });
        }

        private static Codec<PalettedContainer<RegistryEntry<Biome>>> createBiomeCodec(final Registry<Biome> biomeRegistry) {
            return PalettedContainer.createPalettedContainerCodec(
                    biomeRegistry.getIndexedEntries(), biomeRegistry.createEntryCodec(), PalettedContainer.PaletteProvider.BIOME, biomeRegistry.entryOf(BiomeKeys.PLAINS)
            );
        }
    }

    public static Codec<BattleEnvironmentInitialState> codec(final DynamicRegistryManager registryManager) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<BattleEnvironmentInitialState, T>> decode(final DynamicOps<T> ops, final T input) {
                final Optional<MapLike<T>> opt = ops.getMap(input).result();
                if (opt.isEmpty()) {
                    return DataResult.error(() -> "No root map");
                }
                final Optional<Number> optX = ops.getNumberValue(opt.get().get("x")).result();
                if (optX.isEmpty()) {
                    return DataResult.error(() -> "Width not present");
                }
                final int width = optX.get().intValue();
                final Optional<Number> optY = ops.getNumberValue(opt.get().get("y")).result();
                if (optY.isEmpty()) {
                    return DataResult.error(() -> "Height not present");
                }
                final int height = optY.get().intValue();
                final Optional<Number> optZ = ops.getNumberValue(opt.get().get("z")).result();
                if (optZ.isEmpty()) {
                    return DataResult.error(() -> "Depth not present");
                }
                final int depth = optZ.get().intValue();
                final DataResult<List<ChunkSection>> sections = ops.getList(opt.get().get("sections")).flatMap(consumer -> {
                    final DataResult<List<ChunkSection>> sectionList = DataResult.success(new ArrayList<>());
                    consumer.accept(t -> {
                        final DataResult<ChunkSection> deserialize = ChunkSection.deserialize((NbtCompound) ops.convertTo(NbtOps.INSTANCE, t), registryManager.get(RegistryKeys.BIOME));
                        deserialize.apply2stable((section, sections1) -> {
                            sections1.add(section);
                            return sections1;
                        }, sectionList);
                    });
                    return sectionList;
                });
                return sections.flatMap(list -> {
                    if (width * height * depth != list.size()) {
                        return DataResult.error(() -> "section length mismatch!");
                    }
                    return DataResult.success(Pair.of(new BattleEnvironmentInitialState(list.toArray(new ChunkSection[0]), width, height, depth), ops.empty()));
                });
            }

            @Override
            public <T> DataResult<T> encode(final BattleEnvironmentInitialState input, final DynamicOps<T> ops, final T prefix) {
                final RecordBuilder<T> builder = ops.mapBuilder().add("x", ops.createInt(input.width)).add("y", ops.createInt(input.height)).add("z", ops.createInt(input.depth));
                final ListBuilder<T> listBuilder = ops.listBuilder();
                for (final ChunkSection section : input.sections) {
                    listBuilder.add(NbtOps.INSTANCE.convertTo(ops, section.serialize(registryManager.get(RegistryKeys.BIOME))));
                }
                builder.add("sections", listBuilder.build(ops.empty()));
                return builder.build(prefix);
            }
        };
    }
}
