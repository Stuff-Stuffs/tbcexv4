package io.github.stuff_stuffs.tbcexv4.client.internal.ui;

import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.Tbcexv4UiComponents;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.TopmostLayout;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class BattleMenuScreen extends BaseOwoScreen<TopmostLayout> {
    private final BattleHandle handle;

    public BattleMenuScreen(final BattleHandle handle) {
        this.handle = handle;
    }

    @Override
    protected OwoUIAdapter<TopmostLayout> createAdapter() {
        return OwoUIAdapter.create(this, (hSizing, vSizing) -> {
            final StackLayout layout = Containers.stack(Sizing.fill(), Sizing.fill());
            layout.child(Tbcexv4UiComponents.createBackgroundComponent());
            final ParentComponent selector = Tbcexv4UiComponents.createSelectBattleComponent();
            final ParentComponent participantSelector = Tbcexv4UiComponents.createSelectParticipantComponent();
            selector.positioning(Positioning.relative(5, 0));
            layout.child(selector);
            participantSelector.positioning(Positioning.relative(30, 0));
            layout.child(participantSelector);
            final FlowLayout centerPanel = Containers.verticalFlow(Sizing.fill(15), Sizing.content());
            centerPanel.gap(4);
            centerPanel.padding(Insets.of(4));
            Tbcexv4UiComponents.setupPanel(centerPanel);
            centerPanel.positioning(Positioning.relative(50, 50));
            final TopmostLayout topmostLayout = new TopmostLayout(hSizing, vSizing, layout);
            final ButtonComponent buttonComponent = Components.button(Text.of("Inventory"), b -> {
                final Optional<BattleView> watched = Tbcexv4ClientApi.watched();
                if (watched.isEmpty()) {
                    close();
                } else {
                    final Optional<BattleParticipantHandle> controlling = Tbcexv4ClientApi.controlling();
                    if (controlling.isEmpty()) {
                    } else {
                        final BattleParticipantView participant = watched.get().state().participant(controlling.get());
                        if (participant != null) {
                            topmostLayout.push(Tbcexv4UiComponents.inventory(participant));
                        }
                    }
                }
            });
            centerPanel.child(buttonComponent);
            layout.child(centerPanel);
            topmostLayout.keyPress().subscribe((keyCode, scanCode, modifiers) -> {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    if (topmostLayout.stackSize() > 0) {
                        topmostLayout.pop();
                    } else {
                        close();
                    }
                    return true;
                }
                return false;
            });
            return topmostLayout;
        });
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void build(final TopmostLayout rootComponent) {

    }

    @Override
    public void render(final DrawContext context, final int mouseX, final int mouseY, final float delta) {
        if (!Tbcexv4ClientApi.watching().equals(Optional.of(handle))) {
            close();
            return;
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
