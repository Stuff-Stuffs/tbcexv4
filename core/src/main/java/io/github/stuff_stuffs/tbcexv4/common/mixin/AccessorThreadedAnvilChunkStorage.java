package io.github.stuff_stuffs.tbcexv4.common.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface AccessorThreadedAnvilChunkStorage {
    @Accessor(value = "world")
    ServerWorld getWorld();
}
