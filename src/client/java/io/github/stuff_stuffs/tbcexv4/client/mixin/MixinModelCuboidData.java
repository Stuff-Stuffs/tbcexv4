package io.github.stuff_stuffs.tbcexv4.client.mixin;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelCuboidData;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ModelCuboidData.class)
public interface MixinModelCuboidData {
    @Accessor
    String getName();

    @Accessor
    Vector3f getOffset();

    @Accessor
    Vector3f getDimensions();

    @Accessor
    Dilation getExtraSize();

    @Accessor
    boolean getMirror();

    @Accessor
    Vector2f getTextureUV();

    @Accessor
    Vector2f getTextureScale();

    @Accessor
    Set<Direction> getDirections();
}
