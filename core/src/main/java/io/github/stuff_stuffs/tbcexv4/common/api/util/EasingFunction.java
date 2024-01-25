package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.serialization.Codec;

public sealed interface EasingFunction permits BasicEasingFunction {
    Codec<EasingFunction> CODEC = DispatchCodec.<EasingFunction>builder().add(BasicEasingFunction.class, BasicEasingFunction.CODEC).build();
    double remap(double t);
}
