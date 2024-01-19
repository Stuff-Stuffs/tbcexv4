package io.github.stuff_stuffs.tbcexv4.client.api.render.animation;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;

public interface AnimationQueue {
    double enqueue(Animation<BattleRenderState> animation, double minTime, double cutoff);

    BattleRenderState state();
}
