package io.github.stuff_stuffs.tbcexv4.client.mixin;

import net.minecraft.client.model.Dilation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Dilation.class)
public interface AccessorDilation {
    @Accessor
    float getRadiusX();

    @Accessor
    float getRadiusY();

    @Accessor
    float getRadiusZ();
}
