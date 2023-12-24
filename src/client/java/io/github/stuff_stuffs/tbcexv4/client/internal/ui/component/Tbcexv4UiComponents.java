package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BattleUiComponents;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.layers.Layers;
import io.wispforest.owo.ui.parsing.UIParsing;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;

public final class Tbcexv4UiComponents {
    public static void init() {
        Layers.add((hSizing, vSizing) -> {
            final ParentComponent component = BattleUiComponents.createSelectBattleComponent();
            component.sizing(hSizing, vSizing);
            component.id("root");
            return component;
        }, instance -> {
            instance.aggressivePositioning = true;
            instance.alignComponentToHandledScreenCoordinates(instance.adapter.rootComponent, 0, 0);
        }, AbstractInventoryScreen.class);
        UIParsing.registerFactory(Tbcexv4.id("topmost"), TopmostLayout::parse);
    }

    public static Component createBackgroundComponent() {
        BoxComponent component = Components.box(Sizing.fill(), Sizing.fill());
        component.fill(true);
        component.color(Color.ofArgb(0x7F7F7F7F));
        return component;
    }

    public static void setupPanel(ParentComponent component) {
        component.surface(Surface.PANEL);
    }

    private Tbcexv4UiComponents() {
    }
}
