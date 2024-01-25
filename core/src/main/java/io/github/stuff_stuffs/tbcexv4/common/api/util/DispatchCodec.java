package io.github.stuff_stuffs.tbcexv4.common.api.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DispatchCodec<K> implements Codec<K> {
    private final List<Entry<? extends K>> entries;

    private DispatchCodec(List<Entry<? extends K>> entries) {
        this.entries = entries;
    }

    @Override
    public <T> DataResult<Pair<K, T>> decode(DynamicOps<T> ops, T input) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry<? extends K> entry = entries.get(i);
            DataResult<? extends Pair<? extends K, T>> result = entry.decode(ops, input);
            Optional<? extends Pair<? extends K, T>> pair = result.result();
            if(pair.isPresent()) {
                Pair<? extends K, T> value = pair.get();
                return DataResult.success(Pair.of(value.getFirst(), value.getSecond()));
            }
        }
        return DataResult.error(() -> "No matching codecs!");
    }

    @Override
    public <T> DataResult<T> encode(K input, DynamicOps<T> ops, T prefix) {
        for (Entry<? extends K> entry : entries) {
            if(entry.matches(input)) {
                return entry.encode(input, ops, prefix);
            }
        }
        return DataResult.error(() -> "No matching codec for class: " + input.getClass().getName());
    }

    private static final class Entry<K> {
        private final Class<K> clazz;
        private final Codec<K> codec;

        private Entry(Class<K> clazz, Codec<K> codec) {
            this.clazz = clazz;
            this.codec = codec;
        }

        public boolean matches(Object o) {
            return clazz.isInstance(o);
        }

        public <T> DataResult<T> encode(Object input, DynamicOps<T> ops, T prefix) {
            return codec.encode((K)input, ops, prefix);
        }

        public <T> DataResult<Pair<K, T>> decode(DynamicOps<T> ops, T input) {
            return codec.decode(ops, input);
        }
    }

    public static  <K> Builder<K> builder() {
        return new Builder<>();
    }

    public static final class Builder<K> {
        private final List<Entry<? extends K>> entries = new ArrayList<>();

        private Builder() {}

        public <K0 extends K> Builder<K> add(Class<K0> clazz, Codec<K0> codec) {
            entries.add(new Entry<>(clazz, codec));
            return this;
        }

        public DispatchCodec<K> build() {
            return new DispatchCodec<>(List.copyOf(entries));
        }
    }
}
