package io.github.stuff_stuffs.tbcexv4.common.api.battle.turn;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class TurnManagerType<P> {
    private final BiFunction<BattleCodecContext, P, ? extends TurnManager> factory;
    private final Function<BattleCodecContext, Codec<P>> parameterCodecFactory;

    public TurnManagerType(final BiFunction<BattleCodecContext, P, ? extends TurnManager> factory, final Function<BattleCodecContext, Codec<P>> codecFactory) {
        this.factory = factory;
        parameterCodecFactory = codecFactory;
    }

    public TurnManager create(final BattleCodecContext context, final P parameters) {
        return factory.apply(context, parameters);
    }

    public Codec<P> parameterCodec(final BattleCodecContext context) {
        return parameterCodecFactory.apply(context);
    }
}
