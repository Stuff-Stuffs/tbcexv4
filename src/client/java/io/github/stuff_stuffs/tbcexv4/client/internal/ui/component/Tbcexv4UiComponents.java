package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BattleUiComponents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.event.MouseDown;
import io.wispforest.owo.ui.layers.Layers;
import io.wispforest.owo.ui.parsing.UIParsing;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Optional;
import java.util.function.Consumer;

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

    public static StatefulGridLayout<SelectedState> createInventoryEntry(BattleItemStack item, int color, Runnable select) {
        StatefulGridLayout<SelectedState> grid = new StatefulGridLayout<>(Sizing.fill(), Sizing.content(), 1, 3, new SelectedState(Surface.flat(color), Surface.flat(0xFF000000 | color), false));
        grid.surface(Surface.flat(color));
        LabelComponent nameLabel = Components.label(item.item().name());
        nameLabel.horizontalTextAlignment(HorizontalAlignment.CENTER);
        nameLabel.verticalTextAlignment(VerticalAlignment.CENTER);
        nameLabel.sizing(Sizing.fill(40), Sizing.content());
        grid.child(nameLabel, 0, 0);
        LabelComponent countLabel = Components.label(Text.of(String.valueOf(item.count())));
        countLabel.sizing(Sizing.fill(30), Sizing.fixed(40));
        grid.child(countLabel, 0 , 1);
        LabelComponent rarity = Components.label(item.item().rarity().asText());
        rarity.sizing(Sizing.fill(30), Sizing.content());
        grid.child(rarity, 0, 2);
        grid.mouseDown().subscribe((mouseX, mouseY, button) -> {
            select.run();
            return true;
        });
        return grid;
    }

    public static Component

    public static Component createInventoryList(BattleParticipantView participant, Consumer<Optional<InventoryHandle>> consumer) {
        FlowLayout list = Containers.verticalFlow(Sizing.fill(40), Sizing.content());
        boolean even = false;
        Mutable<InventoryHandle> handle = new MutableObject<>(null);
        list.gap(4);
        list.mouseDown().subscribe((mouseX, mouseY, button) -> {
            InventoryHandle h = handle.getValue();
            if(h!=null) {
                consumer.accept(Optional.empty());
                handle.setValue(null);
                StatefulGridLayout<SelectedState> layout = list.childById(StatefulGridLayout.class, h.toString());
                SelectedState data = layout.data();
                layout.surface(data.normal);
                layout.data(new SelectedState(data.normal, data.selectedSurface, false));
                return true;
            }
            return false;
        })
        for (InventoryView.InventoryEntryView view : participant.inventory().entries()) {
            Optional<BattleItemStack> stack = view.stack();
            if(stack.isPresent()) {
                int color = even?0x8F8F8F8F:0x7FAFAFAF;
                StatefulGridLayout<SelectedState> entry = createInventoryEntry(stack.get(), color, () -> {
                    InventoryHandle h = handle.getValue();
                    if (h != null) {
                        StatefulGridLayout<SelectedState> layout = list.childById(StatefulGridLayout.class, h.toString());
                        SelectedState data = layout.data();
                        layout.surface(data.normal);
                        layout.data(new SelectedState(data.normal, data.selectedSurface, false));
                    }
                    StatefulGridLayout<SelectedState> layout = list.childById(StatefulGridLayout.class, view.handle().toString());
                    SelectedState data = layout.data();
                    layout.surface(data.selectedSurface);
                    layout.data(new SelectedState(data.normal, data.selectedSurface, true));
                    handle.setValue(view.handle());
                    consumer.accept(Optional.of(view.handle()));
                });
                entry.id(view.handle().toString());
                list.child(entry);
                even = !even;
            }
        }
        return Containers.verticalScroll(Sizing.content(), Sizing.fill(80), list);
    }

    private record SelectedState(Surface normal, Surface selectedSurface, boolean selected) {
    }

    private Tbcexv4UiComponents() {
    }
}
