package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

public final class BattleItemType<T extends BattleItem> {
    private final Function<BattleCodecContext, Codec<T>> codecFactory;
    private final BiFunction<BattleItemStack, BattleItemStack, Optional<BattleItemStack>> merge;
    private final BiPredicate<T, T> matches;

    public BattleItemType(final Function<BattleCodecContext, Codec<T>> codecFactory, final BiFunction<BattleItemStack, BattleItemStack, Optional<BattleItemStack>> merge, final BiPredicate<T, T> matches) {
        this.codecFactory = codecFactory;
        this.merge = merge;
        this.matches = matches;
    }

    public Codec<T> codec(final BattleCodecContext context) {
        return codecFactory.apply(context);
    }

    public Optional<BattleItemStack> merge(final BattleItemStack first, final BattleItemStack second) {
        if (first.item().type() != this || second.item().type() != this) {
            return Optional.empty();
        }
        return merge.apply(first, second);
    }

    public boolean matches(final BattleItem first, final BattleItem second) {
        if (first.type() != this || second.type() != this) {
            return false;
        }
        //noinspection unchecked
        return matches.test((T) first, (T) second);
    }
}
