package io.github.stuff_stuffs.tbcexv4.client.mixin;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.TextureDimensions;
import net.minecraft.client.model.TexturedModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TexturedModelData.class)
public interface MixinTexturedModelData {
    @Accessor
    ModelData getData();

    @Accessor
    TextureDimensions getDimensions();
}
