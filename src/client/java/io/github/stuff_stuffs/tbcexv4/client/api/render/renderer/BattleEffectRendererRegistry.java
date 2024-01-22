package io.github.stuff_stuffs.tbcexv4.client.api.render.renderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.Identifier;

import java.util.Map;

public class BattleEffectRendererRegistry {
    private static final Map<Identifier, BattleEffectRenderer> REGISTRY = new Object2ReferenceOpenHashMap<>();
    private static final Map<BattleEffectRenderer, Identifier> REVERSE_REGISTRY = new Reference2ObjectOpenHashMap<>();
    public static final BattleEffectRenderer NOOP_RENDERER = (context, state) -> {
    };
    public static final Codec<BattleEffectRenderer> CODEC = Identifier.CODEC.flatXmap(BattleEffectRendererRegistry::result, BattleEffectRendererRegistry::id);


    public static void register(final Identifier id, final BattleEffectRenderer renderer) {
        if (REGISTRY.putIfAbsent(id, renderer) != null) {
            throw new RuntimeException();
        }
        if (REVERSE_REGISTRY.putIfAbsent(renderer, id) != null) {
            REGISTRY.remove(id);
            throw new RuntimeException();
        }
    }

    private static DataResult<BattleEffectRenderer> result(final Identifier id) {
        final BattleEffectRenderer type = REGISTRY.get(id);
        if (type != null) {
            return DataResult.success(type);
        } else {
            return DataResult.error(() -> "Unknown model renderer: " + id);
        }
    }

    public static BattleEffectRenderer get(final Identifier id) {
        return REGISTRY.get(id);
    }

    private static DataResult<Identifier> id(final BattleEffectRenderer renderer) {
        final Identifier identifier = REVERSE_REGISTRY.get(renderer);
        if (identifier != null) {
            return DataResult.success(identifier);
        } else {
            return DataResult.error(() -> "Unregistered ModelRenderer with clazz: " + renderer.getClass().getName());
        }
    }

    public static void init() {
        register(Tbcexv4.id("noop"), NOOP_RENDERER);
    }
}
