package io.github.stuff_stuffs.tbcexv4.client.api.render.animation;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.BattleTraceEvent;

import java.util.Optional;

public interface AnimationFactory {
    Optional<Animation<BattleRenderState>> create(BattleTraceEvent event);
}
