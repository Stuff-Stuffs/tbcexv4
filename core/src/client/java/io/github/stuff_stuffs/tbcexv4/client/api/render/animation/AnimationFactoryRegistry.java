package io.github.stuff_stuffs.tbcexv4.client.api.render.animation;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//TODO phases
public final class AnimationFactoryRegistry {
    private static final List<AnimationFactory> FACTORIES = new ArrayList<>();

    public static void register(final AnimationFactory factory) {
        FACTORIES.add(factory);
    }

    public static Optional<Animation<BattleRenderState>> create(final BattleTraceEvent event) {
        for (final AnimationFactory factory : FACTORIES) {
            final Optional<Animation<BattleRenderState>> animation = factory.create(event);
            if (animation.isPresent()) {
                return animation;
            }
        }
        return Optional.empty();
    }

    private AnimationFactoryRegistry() {
    }
}
