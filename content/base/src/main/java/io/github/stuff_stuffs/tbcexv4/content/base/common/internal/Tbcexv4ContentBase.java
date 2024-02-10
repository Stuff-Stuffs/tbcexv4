package io.github.stuff_stuffs.tbcexv4.content.base.common.internal;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class Tbcexv4ContentBase implements ModInitializer {
    public static final String MOD_ID = "tbcexv4content_base";

    @Override
    public void onInitialize() {
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}
