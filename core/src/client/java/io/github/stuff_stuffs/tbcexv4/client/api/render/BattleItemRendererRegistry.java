package io.github.stuff_stuffs.tbcexv4.client.api.render;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemType;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Map;

public final class BattleItemRendererRegistry {
    private static final Map<BattleItemType<?>, BattleItemRenderer<?>> RENDERERS = new Reference2ObjectOpenHashMap<>();

    public static <T extends BattleItem> void registry(BattleItemType<T> type, BattleItemRenderer<? super T> renderer) {
        if(RENDERERS.putIfAbsent(type, renderer) != null) {
            throw new RuntimeException();
        }
    }

    public static void render(BattleItem item, VertexConsumerProvider vertexConsumers, MatrixStack matrices) {
        BattleItemRenderer<?> renderer = RENDERERS.get(item.type());
        if(renderer==null) {
            return;
        }
        render(renderer, item, vertexConsumers, matrices);
    }

    private static <T extends BattleItem> void render(BattleItemRenderer<? super T> renderer, BattleItem item, VertexConsumerProvider vertexConsumers, MatrixStack matrices) {
        renderer.render((T)item, vertexConsumers, matrices);
    }

    private BattleItemRendererRegistry() {
    }
}
