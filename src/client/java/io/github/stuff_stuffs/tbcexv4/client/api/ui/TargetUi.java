package io.github.stuff_stuffs.tbcexv4.client.api.ui;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.Target;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetChooser;
import io.wispforest.owo.ui.core.Component;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.text.Text;

public interface TargetUi<T extends Target> {
    void targeting(TargetChooser<T> chooser, Context context);

    interface Context {
        void addRenderable(WorldInteraction worldInteraction);

        void addMenu(Text name, Text description, Component component);

        void acceptTarget(Target target);

        BattleView battle();
    }

    interface WorldInteraction {
        void render(WorldRenderContext renderContext);

        double buttonDistance();

        void onButton(Context context);

        void close();
    }
}
