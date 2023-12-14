package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(VersionedChunkStorage.class)
public class MixinVersionedChunkStorage {
    @Inject(method = "needsBlending", at = @At("HEAD"), cancellable = true)
    private void blendingHook(final ChunkPos chunkPos, final int checkRadius, final CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this) instanceof ThreadedAnvilChunkStorage && ((AccessorThreadedAnvilChunkStorage) this).getWorld() instanceof ServerBattleWorld) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getNbt", at = @At("HEAD"), cancellable = true)
    private void readHook(final ChunkPos chunkPos, final CallbackInfoReturnable<CompletableFuture<Optional<NbtCompound>>> cir) {
        if (((Object) this) instanceof ThreadedAnvilChunkStorage && ((AccessorThreadedAnvilChunkStorage) this).getWorld() instanceof ServerBattleWorld) {
            cir.setReturnValue(CompletableFuture.completedFuture(Optional.empty()));
        }
    }

    @Inject(method = "setNbt", at = @At("HEAD"), cancellable = true)
    private void hook(final ChunkPos chunkPos, final NbtCompound nbt, final CallbackInfo ci) {
        if (((Object) this) instanceof ThreadedAnvilChunkStorage && ((AccessorThreadedAnvilChunkStorage) this).getWorld() instanceof ServerBattleWorld) {
            ci.cancel();
        }
    }
}
