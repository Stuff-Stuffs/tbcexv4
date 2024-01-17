package io.github.stuff_stuffs.tbcexv4.client.internal.ui;

import io.github.stuff_stuffs.tbcexv4.client.api.ui.TargetUi;
import io.github.stuff_stuffs.tbcexv4.client.api.ui.TargetUiRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.client.impl.TargetUiContextImpl;
import io.github.stuff_stuffs.tbcexv4.client.internal.Tbcexv4Client;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.Tbcexv4UiComponents;
import io.github.stuff_stuffs.tbcexv4.client.internal.ui.component.TopmostLayout;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.Target;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetChooser;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetType;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BattleTargetingMenu extends BaseOwoScreen<TopmostLayout> {
    private static final List<TargetUiContextImpl> STACK = new ArrayList<>();
    private final TargetUiContextImpl uiContext;

    public BattleTargetingMenu(final TargetUiContextImpl context) {
        uiContext = context;
    }

    @Override
    protected @NotNull OwoUIAdapter<TopmostLayout> createAdapter() {
        return OwoUIAdapter.create(this, (hSizing, vSizing) -> {
            final FlowLayout centerPanel = Containers.verticalFlow(Sizing.fill(15), Sizing.content());
            centerPanel.gap(4);
            centerPanel.padding(Insets.of(4));
            Tbcexv4UiComponents.setupPanel(centerPanel);
            centerPanel.positioning(Positioning.relative(50, 50));
            final TopmostLayout topmostLayout = new TopmostLayout(hSizing, vSizing, centerPanel);
            if (uiContext.plan.canBuild()) {
                final ButtonComponent button = Components.button(Text.of("Ok"), b -> {
                    for (final TargetUiContextImpl context : STACK) {
                        context.close();
                    }
                    STACK.clear();
                    close();
                    for (final BattleAction action : uiContext.plan.build()) {
                        Tbcexv4ClientApi.sendRequest(action);
                    }
                });
                button.tooltip(Text.of("Do the thing!"));
                centerPanel.child(button);
            }
            for (final TargetUiContextImpl.MenuItem item : uiContext.items) {
                final ButtonComponent button = Components.button(item.name(), b -> {
                    final Component component = item.component();
                    component.sizing(Sizing.fill(), Sizing.fill());
                    topmostLayout.push(component);
                });
                button.tooltip(item.description());
                centerPanel.child(button);
            }
            topmostLayout.keyPress().subscribe((keyCode, scanCode, modifiers) -> {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    if (topmostLayout.stackSize() > 0) {
                        topmostLayout.pop();
                    } else {
                        uiContext.menuOpen = false;
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
    public void close() {
        super.close();
        if (!STACK.isEmpty()) {
            if (!STACK.get(STACK.size() - 1).hasWorldRenderables()) {
                STACK.remove(STACK.size() - 1).close();
                if (STACK.isEmpty()) {
                    return;
                }
                final TargetUiContextImpl context = STACK.get(STACK.size() - 1);
                if (context.menuOpen || !context.hasWorldRenderables()) {
                    MinecraftClient.getInstance().setScreen(new BattleTargetingMenu(context));
                }
            }
        }
    }

    @Override
    protected void build(final TopmostLayout rootComponent) {

    }

    public static void initClient() {
        Tbcexv4ClientApi.BATTLE_WATCH_EVENT.register((battleHandle, participantHandle) -> {
            final Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof BattleTargetingMenu) {
                screen.close();
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!STACK.isEmpty()) {
                final TargetUiContextImpl uiContext = STACK.get(STACK.size() - 1);
                uiContext.renderWorld(context);
            }
        });
    }

    public static void inventoryPressed() {
        if (targeting()) {
            final TargetUiContextImpl context = STACK.get(STACK.size() - 1);
            if (!context.menuOpen && !context.items.isEmpty()) {
                context.menuOpen = true;
                MinecraftClient.getInstance().setScreen(new BattleTargetingMenu(context));
            } else {
                final Screen screen = MinecraftClient.getInstance().currentScreen;
                if (screen != null) {
                    context.menuOpen = false;
                    screen.close();
                }
            }
        }
    }

    public static boolean escapePressed() {
        if (!STACK.isEmpty()) {
            STACK.remove(STACK.size() - 1).close();
            if (STACK.isEmpty()) {
                return true;
            }
            final TargetUiContextImpl context = STACK.get(STACK.size() - 1);
            if (context.menuOpen) {
                MinecraftClient.getInstance().setScreen(new BattleTargetingMenu(context));
            }
            return true;
        }
        return false;
    }

    public static void openRoot(final Plan plan) {
        if (!STACK.isEmpty()) {
            throw new RuntimeException();
        }
        MinecraftClient.getInstance().setScreen(null);
        openNested(plan);
    }

    public static boolean targeting() {
        return !STACK.isEmpty();
    }

    private static <T extends Target> void append(final TargetUiContextImpl context, final TargetType<T> type, final TargetChooser<?> chooser) {
        final TargetUi<T> ui = TargetUiRegistry.get(type);
        if (ui != null) {
            //noinspection unchecked
            ui.targeting((TargetChooser<T>) chooser, context);
        }
    }

    private static void openNested(final Plan plan) {
        if (plan.targetTypes().size() == 1) {
            final TargetChooser<?> chooser = plan.ofType(plan.targetTypes().iterator().next());
            final Iterator<? extends Target> iterator = chooser.all();
            if (iterator.hasNext()) {
                final Target target = iterator.next();
                if (!iterator.hasNext()) {
                    openNested(plan.addTarget(target));
                    return;
                }
            }
        }
        final TargetUiContextImpl context = new TargetUiContextImpl(plan, BattleTargetingMenu::openNested, Tbcexv4Client.watched());
        for (final TargetType<?> type : plan.targetTypes()) {
            append(context, type, plan.ofType(type));
        }
        STACK.add(context);
        if (!context.hasWorldRenderables()) {
            context.menuOpen = true;
            MinecraftClient.getInstance().setScreen(new BattleTargetingMenu(context));
        }
    }

    public static boolean onAttack() {
        if (targeting()) {
            final TargetUiContextImpl context = STACK.get(STACK.size() - 1);
            if (!context.menuOpen) {
                double d = Double.POSITIVE_INFINITY;
                TargetUi.WorldInteraction chosen = null;
                for (final TargetUi.WorldInteraction interaction : context.interactions) {
                    final double distance = interaction.buttonDistance();
                    if (distance < d) {
                        d = distance;
                        chosen = interaction;
                    }
                }
                if (chosen != null) {
                    chosen.onButton(context);
                    return true;
                }
            }
        }
        return false;
    }
}
