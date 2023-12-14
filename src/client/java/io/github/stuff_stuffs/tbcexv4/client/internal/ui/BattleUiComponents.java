package io.github.stuff_stuffs.tbcexv4.client.internal.ui;

import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.function.Consumer;

public final class BattleUiComponents {
    public static ParentComponent createSelectBattleComponent() {
        final DropdownComponent dropdownComponent = new FixedDropdownComponent(Sizing.fill(25));
        dropdownComponent.closeWhenNotHovered();
        final TickingParentComponent ticking = new TickingParentComponent(Sizing.content(), Sizing.content(), dropdownComponent);
        ticking.onDraw().subscribe(new TickingParentComponent.OnDraw() {
            private float ticks = 0;
            private Set<BattleHandle> last = new ObjectOpenHashSet<>();

            @Override
            public void onDraw(final OwoUIDrawContext context, final int mouseX, final int mouseY, final float partialTicks, final float delta) {
                ticks += delta;
                if (ticks >= 10) {
                    ticks = 0;
                    final Set<BattleHandle> handles = Tbcexv4ClientApi.possibleControlling();
                    if (!(handles.containsAll(last) && last.containsAll(handles))) {
                        last = Set.copyOf(handles);
                        dropdownComponent.clearChildren();
                        final FlowLayout options = Containers.verticalFlow(Sizing.fill(), Sizing.content());
                        final ScrollContainer<FlowLayout> scroller = Containers.verticalScroll(Sizing.fill(), Sizing.fill(50), options);
                        dropdownComponent.button(Text.of("EXIT"), component -> Tbcexv4ClientApi.requestWatch(null));
                        dropdownComponent.child(scroller);
                        createBattleSelection(last, options::child);
                    }
                }
            }
        });
        return ticking;
    }

    private static void createBattleSelection(final Set<BattleHandle> handles, final Consumer<Component> consumer) {
        for (final BattleHandle handle : handles) {
            final GridLayout grid = Containers.grid(Sizing.fill(), Sizing.fixed(25), 2, 1);
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

    private BattleUiComponents() {
    }
}
