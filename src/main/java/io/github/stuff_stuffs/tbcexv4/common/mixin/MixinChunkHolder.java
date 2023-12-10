package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ChunkSectionExtensions;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public class MixinChunkHolder {
    @Shadow
    @Final
    private HeightLimitView world;

    @Shadow
    @Final
    private LightingProvider lightingProvider;

    @Inject(method = "markForLightUpdate", at = @At("HEAD"), cancellable = true)
    private void hook(final LightType lightType, final int y, final CallbackInfo ci) {
        if (world instanceof ServerBattleWorld) {
            ci.cancel();
        }
    }

    @Inject(method = "flushUpdates", at = @At("HEAD"))
    private void flushHook(final WorldChunk chunk, final CallbackInfo ci) {
        if (chunk.getWorld() instanceof ServerBattleWorld) {
            final ChunkSection[] array = chunk.getSectionArray();
            boolean needsFlush = false;
            for (ChunkSection section : array) {
                final ChunkSectionExtensions extensions = (ChunkSectionExtensions) section;
                if (extensions.tbcexv4$needsFlush()) {
                    extensions.tbcexv4$flush();
                    needsFlush = true;
                }
            }
            if (needsFlush) {
                Tbcexv4.updateAllWatchers(chunk, lightingProvider);
            }
        }
    }
}
