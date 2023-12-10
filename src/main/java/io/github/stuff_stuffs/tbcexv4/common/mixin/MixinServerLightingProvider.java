package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

@Mixin(ServerLightingProvider.class)
public class MixinServerLightingProvider {
    @Shadow
    @Final
    private ThreadedAnvilChunkStorage chunkStorage;

    @Inject(method = "enqueue(IILjava/util/function/IntSupplier;Lnet/minecraft/server/world/ServerLightingProvider$Stage;Ljava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    private void cancelHook(final int x, final int z, final IntSupplier completedLevelSupplier, final ServerLightingProvider.Stage stage, final Runnable task, final CallbackInfo ci) {
        if (((MixinThreadedAnvilChunkStorage) chunkStorage).tbcexv4$world() instanceof ServerBattleWorld) {
            ci.cancel();
        }
    }
}
