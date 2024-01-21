package io.github.stuff_stuffs.tbcexv4.client.mixin;

import net.minecraft.client.model.TextureDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureDimensions.class)
public interface MixinTextureDimensions {
    @Accessor
    int getWidth();

    @Accessor
    int getHeight();
}
