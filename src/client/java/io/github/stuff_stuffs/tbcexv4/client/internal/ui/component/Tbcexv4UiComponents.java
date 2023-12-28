package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.github.stuff_stuffs.tbcexv4.client.internal.ui.BattleUiComponents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.layers.Layers;
import io.wispforest.owo.ui.parsing.UIParsing;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;
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
        final BoxComponent component = Components.box(Sizing.fill(), Sizing.fill());
        component.fill(true);
        component.color(Color.ofArgb(0x7F7F7F7F));
        return component;
    }

    public static void setupPanel(final ParentComponent component) {
        component.surface(Surface.PANEL);
    }

    public static StatefulGridLayout<SelectedState> createInventoryEntry(final BattleItemStack item, final int color, final Runnable select) {
        final StatefulGridLayout<SelectedState> grid = new StatefulGridLayout<>(Sizing.fill(), Sizing.content(), 1, 3, new SelectedState(Surface.flat(color), Surface.flat(0xFF000000 | color), false));
        grid.surface(Surface.flat(color));
        final LabelComponent nameLabel = Components.label(item.item().name());
        nameLabel.horizontalTextAlignment(HorizontalAlignment.CENTER);
        nameLabel.verticalTextAlignment(VerticalAlignment.CENTER);
        nameLabel.sizing(Sizing.fill(40), Sizing.content());
        grid.child(nameLabel, 0, 0);
        final LabelComponent countLabel = Components.label(Text.of(String.valueOf(item.count())));
        countLabel.sizing(Sizing.fill(30), Sizing.fixed(40));
        grid.child(countLabel, 0, 1);
        final LabelComponent rarity = Components.label(item.item().rarity().asText());
        rarity.sizing(Sizing.fill(30), Sizing.content());
        grid.child(rarity, 0, 2);
        grid.mouseDown().subscribe((mouseX, mouseY, button) -> {
            select.run();
            return true;
        });
        return grid;
    }

    public static Component inventory(final BattleParticipantView participant) {
        final FlowLayout layout = Containers.horizontalFlow(Sizing.fill(), Sizing.fill());
        final List<Consumer<Optional<BattleItem>>> toUpdate = new ArrayList<>(0);
        final Component inventoryList = createInventoryList(participant, item -> {
            for (final Consumer<Optional<BattleItem>> consumer : toUpdate) {
                consumer.accept(item);
            }
        });
        final Component itemDisplay = createInventoryItemDisplay(toUpdate::add);
        layout.child(inventoryList);
        layout.child(itemDisplay);
        return layout;
    }

    public static Component createInventoryItemDisplay(final Consumer<Consumer<Optional<BattleItem>>> itemUpdater) {
        final GridLayout layout = Containers.grid(Sizing.fill(60), Sizing.fill(), 2, 1);
        final BattleItemDisplayComponent displayComponent = new BattleItemDisplayComponent();
        displayComponent.sizing(Sizing.fill(), Sizing.fill(65));
        layout.child(displayComponent, 0, 0);
        final FlowLayout desc = Containers.verticalFlow(Sizing.fill(), Sizing.fill(35));
        desc.surface(Surface.TOOLTIP);
        desc.padding(Insets.both(4, 4));
        layout.child(desc, 1, 0);
        itemUpdater.accept(item -> {
            displayComponent.item(item.orElse(null));
            desc.clearChildren();
            if (item.isPresent()) {
                final LabelComponent name = Components.label(item.get().name());
                name.sizing(Sizing.fill(), Sizing.content());
                name.shadow(true);
                name.lineHeight(18);
                name.verticalTextAlignment(VerticalAlignment.TOP);
                name.horizontalTextAlignment(HorizontalAlignment.CENTER);
                desc.child(name);
                final FlowLayout description = Containers.verticalFlow(Sizing.fill(), Sizing.content());
                final ScrollContainer<FlowLayout> descriptionScroll = Containers.verticalScroll(Sizing.fill(), Sizing.fill(), description);
                desc.child(descriptionScroll);
                for (final Text text : item.get().description()) {
                    final LabelComponent descLine = Components.label(text);
                    descLine.verticalTextAlignment(VerticalAlignment.BOTTOM);
                    descLine.horizontalTextAlignment(HorizontalAlignment.LEFT);
                    descLine.sizing(Sizing.fill(), Sizing.content());
                    description.child(descLine);
                }
            }
        });
        return layout;
    }

    public static Component createInventoryList(final BattleParticipantView participant, final Consumer<Optional<BattleItem>> consumer) {
        final FlowLayout list = Containers.verticalFlow(Sizing.fill(40), Sizing.content());
        boolean even = false;
        final Mutable<InventoryHandle> handle = new MutableObject<>(null);
        list.gap(4);
        list.mouseDown().subscribe((mouseX, mouseY, button) -> {
            final InventoryHandle h = handle.getValue();
            if (h != null) {
                consumer.accept(Optional.empty());
                handle.setValue(null);
                final StatefulGridLayout<SelectedState> layout = list.childById(StatefulGridLayout.class, h.toString());
                final SelectedState data = layout.data();
                layout.surface(data.normal);
                layout.data(new SelectedState(data.normal, data.selectedSurface, false));
                return true;
            }
            return false;
        });
        for (final InventoryView.InventoryEntryView view : participant.inventory().entries()) {
            final Optional<BattleItemStack> stack = view.stack();
            if (stack.isPresent()) {
                final int color = even ? 0x7F8F8F8F : 0x7FAFAFAF;
                final StatefulGridLayout<SelectedState> entry = createInventoryEntry(stack.get(), color, () -> {
                    final InventoryHandle h = handle.getValue();
                    if (h != null) {
                        final StatefulGridLayout<SelectedState> layout = list.childById(StatefulGridLayout.class, h.toString());
                        final SelectedState data = layout.data();
                        layout.surface(data.normal);
                        layout.data(new SelectedState(data.normal, data.selectedSurface, false));
                    }
                    final StatefulGridLayout<SelectedState> layout = list.childById(StatefulGridLayout.class, view.handle().toString());
                    final SelectedState data = layout.data();
                    layout.surface(data.selectedSurface);
                    layout.data(new SelectedState(data.normal, data.selectedSurface, true));
                    handle.setValue(view.handle());
                    consumer.accept(Optional.of(stack.get().item()));
                });
                entry.id(view.handle().toString());
                list.child(entry);
                even = !even;
            }
        }
        return Containers.verticalScroll(Sizing.content(), Sizing.fill(80), list);
    }

    public record SelectedState(Surface normal, Surface selectedSurface, boolean selected) {
    }

    private Tbcexv4UiComponents() {
    }
}
