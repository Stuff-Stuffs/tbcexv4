package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.Sizing;

public class StatefulGridLayout<T> extends GridLayout {
    private T data;
    public StatefulGridLayout(Sizing horizontalSizing, Sizing verticalSizing, int rows, int columns, T data) {
        super(horizontalSizing, verticalSizing, rows, columns);
        this.data = data;
    }

    public T data() {
        return data;
    }

    public void data(T data) {
        this.data = data;
    }
}
