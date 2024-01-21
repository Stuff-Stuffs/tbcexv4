package io.github.stuff_stuffs.tbcexv4.client.mixin;

import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(EntityModelLoader.class)
public interface MixinEntityModelLoader {
    @Accessor
    Map<EntityModelLayer, TexturedModelData> getModelParts();
}
