package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.wispforest.owo.ui.base.BaseParentComponent;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.parsing.UIModelParsingException;
import io.wispforest.owo.ui.parsing.UIParsing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TopmostLayout extends BaseParentComponent {
    private final ObjectList<Component> stack;
    private Component base;

    public TopmostLayout(final Sizing horizontalSizing, final Sizing verticalSizing, final Component base) {
        super(horizontalSizing, verticalSizing);
        stack = new ObjectArrayList<>();
        this.base = base;
    }

    @Override
    public void draw(final OwoUIDrawContext context, final int mouseX, final int mouseY, final float partialTicks, final float delta) {
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        drawChildren(context, mouseX, mouseY, partialTicks, delta, List.of(top()));
    }

    @Override
    protected int determineHorizontalContentSize(final Sizing sizing) {
        return top().fullSize().width() + padding.get().horizontal();
    }

    @Override
    protected int determineVerticalContentSize(final Sizing sizing) {
        return top().fullSize().height() + padding.get().vertical();
    }

    @Override
    public void layout(final Size space) {
        top().inflate(calculateChildSpace(space));
        top().mount(this, childMountX(), childMountY());
    }

    protected int childMountX() {
        return x + top().margins().get().left() + padding.get().left();
    }

    protected int childMountY() {
        return y + top().margins().get().top() + padding.get().top();
    }

    @Override
    public List<Component> children() {
        return List.of(top());
    }

    public void base(final Component component) {
        if (stack.isEmpty()) {
            base.dismount(DismountReason.REMOVED);
        }
        base = component;
        updateLayout();
    }

    public void push(final Component component) {
        stack.add(component);
        if (stack.size() == 1) {
            base.dismount(DismountReason.REMOVED);
        } else {
            stack.get(stack.size() - 2).dismount(DismountReason.REMOVED);
        }
        updateLayout();
    }

    private Component top() {
        if (stack.isEmpty()) {
            return base;
        }
        return stack.get(stack.size() - 1);
    }

    public void pop() {
        if (stack.isEmpty()) {
            throw new RuntimeException("Cannot pop base of TopmostLayout");
        }
        final Component component = stack.remove(stack.size() - 1);
        component.dismount(DismountReason.REMOVED);
        updateLayout();
    }

    public int stackSize() {
        return stack.size();
    }

    @Override
    public ParentComponent removeChild(final Component child) {
        if (child == base) {
            throw new RuntimeException("Cannot remove base of TopmostLayout");
        }
        final int index = stack.lastIndexOf(child);
        if (index < 0) {
            return this;
        }
        final Component component = stack.remove(index);
        component.dismount(DismountReason.REMOVED);
        if (index == stack.size()) {
            updateLayout();
        }
        return this;
    }

    @Override
    public void parseProperties(final UIModel model, final Element element, final Map<String, Element> children) {
        super.parseProperties(model, element, children);
        final Component base = UIParsing.get(children, "base", e -> model.parseComponent(Component.class, e)).orElseGet(() -> {
            final BoxComponent box = new BoxComponent(Sizing.fill(), Sizing.fill());
            box.color(new Color(0.7F, 0.7F, 0.7F, 0.35F));
            box.fill(true);
            return box;
        });
        base(base);
        try {
            final var components = UIParsing
                    .get(children, "stack", e -> UIParsing.<Element>allChildrenOfType(e, Node.ELEMENT_NODE))
                    .orElse(Collections.emptyList());

            for (final var child : components) {
                push(model.parseComponent(Component.class, child));
            }
        } catch (final UIModelParsingException exception) {
            throw new UIModelParsingException("Could not initialize container child", exception);
        }
    }

    public static TopmostLayout parse(final Element element) {
        final BoxComponent box = new BoxComponent(Sizing.fill(), Sizing.fill());
        box.color(new Color(0.7F, 0.7F, 0.7F, 0.35F));
        box.fill(true);
        return new TopmostLayout(Sizing.content(), Sizing.content(), box);
    }
}
