package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.wispforest.owo.ui.container.WrappingParentComponent;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;

public class MinSizeComponent<C extends Component> extends WrappingParentComponent<C> {
    private int maxVerticalSizePercent;
    private int maxHorizontalSizePercent;

    public MinSizeComponent(final Sizing horizontalSizing, final Sizing verticalSizing, final C child) {
        super(horizontalSizing, verticalSizing, child);
        maxVerticalSizePercent = 100;
        maxHorizontalSizePercent = 100;
    }

    @Override
    public void draw(final OwoUIDrawContext context, final int mouseX, final int mouseY, final float partialTicks, final float delta) {
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        drawChildren(context, mouseX, mouseY, partialTicks, delta, childView);
    }


    public void maxVerticalSize(final int percent) {
        maxHorizontalSizePercent = percent;
        notifyParentIfMounted();
    }

    public void maxHorizontalSize(final int percent) {
        maxHorizontalSizePercent = percent;
        notifyParentIfMounted();
    }

    @Override
    protected int determineVerticalContentSize(final Sizing sizing) {
        return Math.min(super.determineVerticalContentSize(sizing), (int) (space.height() * (maxVerticalSizePercent / 100.0)));
    }

    @Override
    protected int determineHorizontalContentSize(final Sizing sizing) {
        return Math.min(super.determineHorizontalContentSize(sizing), (int) (space.width() * (maxHorizontalSizePercent / 100.0)));
    }
}
