package io.github.stuff_stuffs.tbcexv4.client.api.render;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public interface BattleItemRenderer<T extends BattleItem> {
    void render(T item, VertexConsumerProvider vertexConsumers, MatrixStack matrices);
}
