package io.github.stuff_stuffs.tbcexv4.client.mixin;

import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BattleTargetingMenu;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "openGameMenu", at = @At("HEAD"), cancellable = true)
    private void targetingHook(final boolean pauseOnly, final CallbackInfo ci) {
        if (BattleTargetingMenu.escapePressed()) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void attackHook(final CallbackInfoReturnable<Boolean> cir) {
        if (BattleTargetingMenu.onAttack()) {
            cir.setReturnValue(true);
        }
    }
}
