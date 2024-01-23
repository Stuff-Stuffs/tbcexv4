package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SerializingRegionBasedStorage.class)
public class MixinSerializingRegionBasedStorage {
    @Shadow
    @Final
    protected HeightLimitView world;

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void cancelSave(final ChunkPos pos, final CallbackInfo ci) {
        if (world instanceof ServerBattleWorld) {
            ci.cancel();
        }
    }
}
