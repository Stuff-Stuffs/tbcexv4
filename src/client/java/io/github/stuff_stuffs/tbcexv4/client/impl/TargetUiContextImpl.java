package io.github.stuff_stuffs.tbcexv4.client.impl;

import io.github.stuff_stuffs.tbcexv4.client.api.TargetUi;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.Plan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.Target;
import io.wispforest.owo.ui.core.Component;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TargetUiContextImpl implements TargetUi.Context {
    public final Plan plan;
    private final Consumer<Plan> consumer;
    private final List<TargetUi.WorldInteraction> interactions = new ArrayList<>();
    public final List<MenuItem> items = new ArrayList<>();
    public boolean menuOpen = false;
    private final BattleView battle;

    public TargetUiContextImpl(final Plan plan, final Consumer<Plan> consumer, BattleView battle) {
        this.plan = plan;
        this.consumer = consumer;
        this.battle = battle;
    }

    @Override
    public void addRenderable(final TargetUi.WorldInteraction worldInteraction) {
        interactions.add(worldInteraction);
    }

    @Override
    public void addMenu(final Text name, final Text description, final Component component) {
        items.add(new MenuItem(name, description, component));
    }

    @Override
    public void acceptTarget(final Target target) {
        consumer.accept(plan.addTarget(target));
    }

    @Override
    public BattleView battle() {
        return battle;
    }

    public void renderWorld(final WorldRenderContext context) {
        for (final TargetUi.WorldInteraction interaction : interactions) {
            interaction.render(context);
        }
    }

    public boolean hasWorldRenderables() {
        return !interactions.isEmpty();
    }

    public void close() {
        for (final TargetUi.WorldInteraction interaction : interactions) {
            interaction.close();
        }
    }

    public record MenuItem(Text name, Text description, Component component) {
    }
}
