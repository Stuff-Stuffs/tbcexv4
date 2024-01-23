package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.wispforest.owo.ui.container.WrappingParentComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;

public class StatefulWrappingComponent<T, C extends Component> extends WrappingParentComponent<C> {
    private T data;

    public StatefulWrappingComponent(final Sizing horizontalSizing, final Sizing verticalSizing, final C child, final T data) {
        super(horizontalSizing, verticalSizing, child);
        this.data = data;
    }

    public T data() {
        return data;
    }

    public void data(final T data) {
        this.data = data;
    }

    @Override
    public void draw(final OwoUIDrawContext context, final int mouseX, final int mouseY, final float partialTicks, final float delta) {
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        drawChildren(context, mouseX, mouseY, partialTicks, delta, childView);
    }
}
