package io.github.stuff_stuffs.tbcexv4.client.mixin;

import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.client.internal.Tbcexv4Client;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends Screen {
    protected MixinInventoryScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void hookScreenSwitch(CallbackInfo ci) {
        final Optional<BattleHandle> watching = Tbcexv4ClientApi.watching();
        if(watching.isPresent() && client.interactionManager.getCurrentGameMode()== GameMode.SPECTATOR) {
            client.setScreen();
            ci.cancel();
        }
    }
}
