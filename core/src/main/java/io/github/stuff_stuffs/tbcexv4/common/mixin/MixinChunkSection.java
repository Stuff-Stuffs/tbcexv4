package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.internal.world.ChunkSectionExtensions;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import org.spongepowered.asm.mixin.*;

@Mixin(ChunkSection.class)
public class MixinChunkSection implements ChunkSectionExtensions {
    @Mutable
    @Shadow
    @Final
    private PalettedContainer<BlockState> blockStateContainer;

    @Shadow
    private ReadableContainer<RegistryEntry<Biome>> biomeContainer;

    @Shadow
    private short nonEmptyBlockCount;
    @Shadow
    private short randomTickableBlockCount;
    @Shadow
    private short nonEmptyFluidCount;
    @Unique
    private boolean tbcexv4$needsFlush;

    @Override
    public void tbcexv4$copy(final short nonEmptyBlockCount, final short randomTickableCount, final short nonEmptyFluidCount, final PalettedContainer<BlockState> blocks, final ReadableContainer<RegistryEntry<Biome>> biomes) {
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        randomTickableBlockCount = randomTickableCount;
        this.nonEmptyFluidCount = nonEmptyFluidCount;
        blockStateContainer = blocks.copy();
        biomeContainer = biomes;
    }

    @Override
    public void tbcexv4$setNeedsFlush() {
        tbcexv4$needsFlush = true;
    }

    @Override
    public boolean tbcexv4$needsFlush() {
        return tbcexv4$needsFlush;
    }

    @Override
    public void tbcexv4$flush() {
        tbcexv4$needsFlush = false;
    }
}
