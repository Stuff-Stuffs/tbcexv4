package io.github.stuff_stuffs.tbcexv4.client.internal.ui;

import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.Tbcexv4UiComponents;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.TopmostLayout;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;

import java.util.Optional;
import java.util.function.BiFunction;

public class BattleMenuScreen extends BaseOwoScreen<TopmostLayout> {
    private final BattleHandle handle;

    public BattleMenuScreen(BattleHandle handle) {
        this.handle = handle;
    }

    @Override
    protected OwoUIAdapter<TopmostLayout> createAdapter() {
        return OwoUIAdapter.create(this, new BiFunction<Sizing, Sizing, TopmostLayout>() {
            @Override
            public TopmostLayout apply(Sizing hSizing, Sizing vSizing) {
                StackLayout layout = Containers.stack(Sizing.fill(), Sizing.fill());
                layout.child(Tbcexv4UiComponents.createBackgroundComponent());
                FlowLayout centerPanel = Containers.verticalFlow(Sizing.fill(15), Sizing.content());
                centerPanel.gap(1);
                Tbcexv4UiComponents.setupPanel(centerPanel);
                centerPanel.positioning(Positioning.relative(50, 50));
                return new TopmostLayout(hSizing, vSizing, layout);
            }
        });
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void build(TopmostLayout rootComponent) {

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(!Tbcexv4ClientApi.watching().equals(Optional.of(handle))) {
            close();
            return;
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private static Component createInventory
}
