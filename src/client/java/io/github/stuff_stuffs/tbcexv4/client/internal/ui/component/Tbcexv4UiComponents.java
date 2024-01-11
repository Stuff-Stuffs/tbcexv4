package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.layers.Layers;
import io.wispforest.owo.ui.parsing.UIParsing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class Tbcexv4UiComponents {
    public static void init() {
        Layers.add((hSizing, vSizing) -> {
            final ParentComponent component = createSelectBattleComponent();
            component.id("root");
            return component;
        }, instance -> {
            instance.aggressivePositioning = true;
        }, InventoryScreen.class, CreativeInventoryScreen.class);
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

    public static StatefulWrappingComponent<SelectedState, GridLayout> createInventoryEntry(final BattleItemStack item, final int color, final Runnable select) {
        final StatefulWrappingComponent<SelectedState, GridLayout> grid = new StatefulWrappingComponent<>(Sizing.fill(), Sizing.content(), Containers.grid(Sizing.fill(), Sizing.content(), 1, 3), new SelectedState(Surface.flat(color), Surface.flat(0xFF000000 | color), false));
        grid.child().surface(Surface.flat(color));
        final LabelComponent nameLabel = Components.label(item.item().name());
        nameLabel.horizontalTextAlignment(HorizontalAlignment.CENTER);
        nameLabel.verticalTextAlignment(VerticalAlignment.CENTER);
        nameLabel.sizing(Sizing.fill(40), Sizing.content());
        grid.child().child(nameLabel, 0, 0);
        final LabelComponent countLabel = Components.label(Text.of(String.valueOf(item.count())));
        countLabel.sizing(Sizing.fill(30), Sizing.fixed(40));
        grid.child().child(countLabel, 0, 1);
        final LabelComponent rarity = Components.label(item.item().rarity().asText());
        rarity.sizing(Sizing.fill(30), Sizing.content());
        grid.child().child(rarity, 0, 2);
        grid.child().mouseDown().subscribe((mouseX, mouseY, button) -> {
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
        final FlowLayout list = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        final ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.content(), Sizing.fill(80), list);
        final GridLayout header = Containers.grid(Sizing.content(), Sizing.content(), 1, 3);
        final LabelComponent nameLabel = Components.label(Text.of("Name"));
        nameLabel.horizontalTextAlignment(HorizontalAlignment.CENTER);
        nameLabel.verticalTextAlignment(VerticalAlignment.CENTER);
        nameLabel.sizing(Sizing.fill(40), Sizing.content());
        header.child(nameLabel, 0, 0);
        final LabelComponent countLabel = Components.label(Text.of("Count"));
        countLabel.sizing(Sizing.fill(30), Sizing.fixed(20));
        header.child(countLabel, 0, 1);
        final LabelComponent rarity = Components.label(Text.of("Rarity"));
        rarity.sizing(Sizing.fill(30), Sizing.content());
        header.child(rarity, 0, 2);
        final FlowLayout withHeader = Containers.verticalFlow(Sizing.fill(40), Sizing.content());
        withHeader.child(header);
        withHeader.child(scroll);
        boolean even = false;
        final Mutable<InventoryHandle> handle = new MutableObject<>(null);
        list.gap(4);
        list.mouseDown().subscribe((mouseX, mouseY, button) -> {
            final InventoryHandle h = handle.getValue();
            if (h != null) {
                consumer.accept(Optional.empty());
                handle.setValue(null);
                final StatefulWrappingComponent<SelectedState, GridLayout> layout = list.childById(StatefulWrappingComponent.class, h.toString());
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
                final StatefulWrappingComponent<SelectedState, GridLayout> entry = createInventoryEntry(stack.get(), color, () -> {
                    final InventoryHandle h = handle.getValue();
                    if (h != null) {
                        final StatefulWrappingComponent<SelectedState, GridLayout> layout = list.childById(StatefulWrappingComponent.class, h.toString());
                        final SelectedState data = layout.data();
                        layout.surface(data.normal);
                        layout.data(new SelectedState(data.normal, data.selectedSurface, false));
                    }
                    final StatefulWrappingComponent<SelectedState, GridLayout> layout = list.childById(StatefulWrappingComponent.class, view.handle().toString());
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
        return withHeader;
    }

    public record SelectedState(Surface normal, Surface selectedSurface, boolean selected) {
    }

    public static ParentComponent createSelectParticipantComponent() {
        final GridLayout layout = Containers.grid(Sizing.fill(25), Sizing.content(5), 1, 1);
        layout.surface(Surface.PANEL);
        layout.mouseEnter().subscribe(() -> {
            final DropdownComponent dropdownComponent = new FixedDropdownComponent(Sizing.fill(25));
            layout.child(dropdownComponent, 0, 0);
            final FlowLayout options = Containers.verticalFlow(Sizing.fill(), Sizing.content());
            options.gap(4);
            final ScrollContainer<FlowLayout> scroller = Containers.verticalScroll(Sizing.fill(), Sizing.fill(50), options);
            for (final BattleParticipantHandle handle : Tbcexv4ClientApi.possibleControlling()) {
                final GridLayout grid = Containers.grid(Sizing.fill(), Sizing.fixed(25), 1, 1);
                grid.surface(Surface.TOOLTIP);
                grid.child(Components.label(Text.of(handle.toString())).cursorStyle(CursorStyle.HAND), 0, 0);
                grid.cursorStyle(CursorStyle.HAND);
                grid.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        Tbcexv4ClientApi.tryControl(handle);
                        MinecraftClient.getInstance().setScreen(null);
                        return true;
                    }
                    return false;
                });
                options.child(grid);
            }
            dropdownComponent.child(scroller);
        });
        layout.mouseLeave().subscribe(() -> layout.removeChild(0, 0));
        return layout;
    }

    public static ParentComponent createSelectBattleComponent() {
        final GridLayout layout = Containers.grid(Sizing.fill(25), Sizing.content(5), 1, 1);
        layout.surface(Surface.PANEL);
        layout.mouseEnter().subscribe(() -> {
            final Set<BattleHandle> handles = Tbcexv4ClientApi.possibleWatching();
            final DropdownComponent dropdownComponent = new FixedDropdownComponent(Sizing.fill(25));
            layout.child(dropdownComponent, 0, 0);
            final FlowLayout options = Containers.verticalFlow(Sizing.fill(), Sizing.content());
            options.gap(4);
            final ScrollContainer<FlowLayout> scroller = Containers.verticalScroll(Sizing.fill(), Sizing.fill(50), options);
            final Optional<BattleHandle> watching = Tbcexv4ClientApi.watching();
            if (watching.isPresent()) {
                options.child(Components.button(Text.of("EXIT"), component -> Tbcexv4ClientApi.requestWatch(null)));
            }
            dropdownComponent.child(scroller);
            createBattleSelection(handles, options::child);
        });
        layout.mouseLeave().subscribe(() -> layout.removeChild(0, 0));
        return layout;
    }

    private static void createBattleSelection(final Set<BattleHandle> handles, final Consumer<Component> consumer) {
        for (final BattleHandle handle : handles) {
            final GridLayout grid = Containers.grid(Sizing.fill(), Sizing.fixed(25), 2, 1);
            grid.surface(Surface.TOOLTIP);
            grid.child(Components.label(Text.of(handle.sourceWorld().getValue().toString())).cursorStyle(CursorStyle.HAND), 0, 0);
            grid.child(Components.label(Text.of(handle.id().toString())).cursorStyle(CursorStyle.HAND), 1, 0);
            grid.cursorStyle(CursorStyle.HAND);
            grid.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    Tbcexv4ClientApi.requestWatch(handle);
                    return true;
                }
                return false;
            });
            consumer.accept(grid);
        }
    }

    private Tbcexv4UiComponents() {
    }
}
