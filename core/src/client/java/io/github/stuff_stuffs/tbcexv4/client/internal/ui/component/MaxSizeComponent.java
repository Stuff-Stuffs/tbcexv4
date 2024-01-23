package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.wispforest.owo.ui.container.WrappingParentComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;

public class MaxSizeComponent<C extends Component> extends WrappingParentComponent<C> {
    private int minVerticalSizePercent;
    private int minHorizontalSizePercent;

    public MaxSizeComponent(final Sizing horizontalSizing, final Sizing verticalSizing, final C child) {
        super(horizontalSizing, verticalSizing, child);
        minVerticalSizePercent = 100;
        minHorizontalSizePercent = 100;
    }

    @Override
    public void draw(final OwoUIDrawContext context, final int mouseX, final int mouseY, final float partialTicks, final float delta) {
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        drawChildren(context, mouseX, mouseY, partialTicks, delta, childView);
    }

    public void minVerticalSize(final int percent) {
        minHorizontalSizePercent = percent;
        notifyParentIfMounted();
    }

    public void minHorizontalSize(final int percent) {
        minHorizontalSizePercent = percent;
        notifyParentIfMounted();
    }

    @Override
    protected int determineVerticalContentSize(final Sizing sizing) {
        return Math.max(super.determineVerticalContentSize(sizing), (int) (sizing.value * (minVerticalSizePercent / 100.0)));
    }

    @Override
    protected int determineHorizontalContentSize(final Sizing sizing) {
        return Math.max(super.determineHorizontalContentSize(sizing), (int) (sizing.value * (minHorizontalSizePercent / 100.0)));
    }
}
