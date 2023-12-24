package io.github.stuff_stuffs.tbcexv4.client.internal.ui;

import io.wispforest.owo.ui.container.WrappingParentComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.util.EventSource;
import io.wispforest.owo.util.EventStream;

public class TickingParentComponent<C extends Component> extends WrappingParentComponent<C> {
    private final EventStream<OnDraw> onDraw;

    public TickingParentComponent(final Sizing horizontalSizing, final Sizing verticalSizing, final C child) {
        super(horizontalSizing, verticalSizing, child);
        onDraw = new EventStream<>(draws -> (context, mouseX, mouseY, partialTicks, delta) -> {
            for (final OnDraw draw : draws) {
                draw.onDraw(context, mouseX, mouseY, partialTicks, delta);
            }
        });
    }

    public EventSource<OnDraw> onDraw() {
        return onDraw.source();
    }

    @Override
    public void draw(final OwoUIDrawContext context, final int mouseX, final int mouseY, final float partialTicks, final float delta) {
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        this.drawChildren(context, mouseX, mouseY, partialTicks, delta, this.childView);
        onDraw.sink().onDraw(context, mouseX, mouseY, partialTicks, delta);
    }

    public interface OnDraw {
        void onDraw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta);
    }
}
