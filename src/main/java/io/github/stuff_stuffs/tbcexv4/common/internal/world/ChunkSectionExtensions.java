package io.github.stuff_stuffs.tbcexv4.common.internal.world;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;

public interface ChunkSectionExtensions {
    void tbcexv4$copy(PalettedContainer<BlockState> blocks, ReadableContainer<RegistryEntry<Biome>> biomes);

    void tbcexv4$setNeedsFlush();

    boolean tbcexv4$needsFlush();

    void tbcexv4$flush();
}
