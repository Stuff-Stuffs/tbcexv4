package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.ChunkDataList;
import net.minecraft.world.storage.EntityChunkDataAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(EntityChunkDataAccess.class)
public class MixinEntityChunkDataAccess {
    @Shadow
    @Final
    private ServerWorld world;

    @Inject(method = "readChunkData", at = @At("HEAD"), cancellable = true)
    private void hook(final ChunkPos pos, final CallbackInfoReturnable<CompletableFuture<ChunkDataList<Entity>>> cir) {
        if (world instanceof ServerBattleWorld) {
            cir.setReturnValue(CompletableFuture.completedFuture(new ChunkDataList<>(pos, List.of())));
        }
    }

    @Inject(method = "writeChunkData", at = @At("HEAD"), cancellable = true)
    private void writeHook(final ChunkDataList<Entity> dataList, final CallbackInfo ci) {
        if (world instanceof ServerBattleWorld) {
            ci.cancel();
        }
    }
}
