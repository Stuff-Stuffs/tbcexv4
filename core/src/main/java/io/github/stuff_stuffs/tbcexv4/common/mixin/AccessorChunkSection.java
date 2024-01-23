package io.github.stuff_stuffs.tbcexv4.common.mixin;

import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkSection.class)
public interface AccessorChunkSection {
    @Accessor(value = "nonEmptyBlockCount")
    short getNonEmptyBlockCount();

    @Accessor(value = "randomTickableBlockCount")
    short getRandomTickableBlockCount();

    @Accessor(value = "nonEmptyFluidCount")
    short getNonEmptyFluidCount();
}
