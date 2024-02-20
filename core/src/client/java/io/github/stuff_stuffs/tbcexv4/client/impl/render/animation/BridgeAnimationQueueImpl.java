package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation;

import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.BattleRenderStateImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state.bridge.BattleRenderStateViewImpl;

public class BridgeAnimationQueueImpl extends AnimationQueueImpl {
    @Override
    protected BattleRenderStateImpl createState() {
        return new BattleRenderStateViewImpl();
    }
}
