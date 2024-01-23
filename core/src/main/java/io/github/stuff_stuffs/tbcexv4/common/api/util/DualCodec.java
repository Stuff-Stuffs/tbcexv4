package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class DualCodec<T> implements Codec<T> {
    private final Codec<T> preferred;
    private final Codec<T> fallback;

    public DualCodec(final Codec<T> preferred, final Codec<T> fallback) {
        this.preferred = preferred;
        this.fallback = fallback;
    }

    @Override
    public <T0> DataResult<Pair<T, T0>> decode(final DynamicOps<T0> ops, final T0 input) {
        final DataResult<Pair<T, T0>> result = preferred.decode(ops, input);
        if (result.result().isPresent()) {
            return result;
        }
        return fallback.decode(ops, input);
    }

    @Override
    public <T0> DataResult<T0> encode(final T input, final DynamicOps<T0> ops, final T0 prefix) {
        final DataResult<T0> encode = preferred.encode(input, ops, prefix);
        if (encode.result().isPresent()) {
            return encode;
        }
        return fallback.encode(input, ops, prefix);
    }
}
