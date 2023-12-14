package io.github.stuff_stuffs.tbcexv4.client.internal.ui;

import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;

public class FixedDropdownComponent extends DropdownComponent {
    public FixedDropdownComponent(final Sizing horizontalSizing) {
        super(horizontalSizing);
    }

    @Override
    public FlowLayout clearChildren() {
        final FlowLayout layout = super.clearChildren();
        entries.clearChildren();
        child(entries);
        return layout;
    }
}
