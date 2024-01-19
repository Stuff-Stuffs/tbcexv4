package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;

import java.util.List;
import java.util.Set;

public interface ModelRenderState extends RenderState {
    ModelRenderState getChild(String id);

    void addChild(String id);

    void removeChild(String id);

    Set<String> children();

    interface ModelLiftingPredicate {
        VisitPathResult visit(ModelRenderState state, List<String> path, AnimationContext context);
    }

    enum VisitPathResult {
        DESCEND(true, false),
        DESCEND_ACCEPT(true, true),
        ACCEPT(false, true),
        SKIP(false, false);
        public final boolean descend;
        public final boolean accept;

        VisitPathResult(final boolean descend, final boolean accept) {
            this.descend = descend;
            this.accept = accept;
        }
    }
}
