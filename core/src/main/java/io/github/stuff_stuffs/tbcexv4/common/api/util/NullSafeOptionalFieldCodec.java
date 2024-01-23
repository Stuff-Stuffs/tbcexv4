package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.serialization.*;

import java.util.Optional;
import java.util.stream.Stream;

public class NullSafeOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
    private final String name;
    private final Codec<A> elementCodec;

    public NullSafeOptionalFieldCodec(final String name, final Codec<A> elementCodec) {
        this.name = name;
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<Optional<A>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        final T value = input.get(name);
        if (value == null) {
            return DataResult.success(Optional.empty());
        }
        final DataResult<A> parsed = elementCodec.parse(ops, value);
        if (parsed.result().isPresent()) {
            return parsed.map(Optional::ofNullable);
        }
        return DataResult.success(Optional.empty());
    }

    @Override
    public <T> RecordBuilder<T> encode(final Optional<A> input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
        if (input.isPresent()) {
            return prefix.add(name, elementCodec.encodeStart(ops, input.get()));
        }
        return prefix;
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return Stream.of(ops.createString(name));
    }
}
