package io.github.stuff_stuffs.tbcexv4.common.api.battle.action;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;

import java.util.function.Function;

public final class BattleActionType<T extends BattleAction> {
    private final Function<BattleCodecContext, Codec<T>> codecFactory;

    public BattleActionType(final Function<BattleCodecContext, Codec<T>> codecFactory) {
        this.codecFactory = codecFactory;
    }

    public Codec<T> codec(final BattleCodecContext manager) {
        return codecFactory.apply(manager);
    }
}
