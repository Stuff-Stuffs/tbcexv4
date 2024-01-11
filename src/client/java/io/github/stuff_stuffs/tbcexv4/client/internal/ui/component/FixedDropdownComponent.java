package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Sizing;

public class FixedDropdownComponent extends DropdownComponent {
    public FixedDropdownComponent(final Sizing horizontalSizing) {
        super(horizontalSizing);
    }

    @Override
    public FlowLayout clearChildren() {
        for (final Component child : children) {
            if (child != entries) {
                child.dismount(DismountReason.REMOVED);
            }
        }
        entries.clearChildren();
        children.clear();
        child(entries);
        updateLayout();
        return this;
    }
}
