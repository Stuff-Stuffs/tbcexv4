package io.github.stuff_stuffs.tbcexv4.client.mixin;

import net.minecraft.client.model.ModelCuboidData;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(ModelPartData.class)
public interface MixinModelPartData {
    @Accessor(value = "cuboidData")
    List<ModelCuboidData> getCuboidData();

    @Accessor(value = "children")
    Map<String, ModelPartData> getChildren();

    @Accessor(value = "rotationData")
    ModelTransform getRotationData();
}
