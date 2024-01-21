package io.github.stuff_stuffs.tbcexv4.client.impl.render;

import io.github.stuff_stuffs.tbcexv4.client.api.render.BattleRenderContext;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.ClientBattleImpl;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.joml.Vector4f;

import java.util.Arrays;

public class BattleRenderContextImpl implements BattleRenderContext {
    private static final int LIGHT_CACHE_BITS = 10;
    private static final int LIGHT_CACHE_SIZE = 1 << (LIGHT_CACHE_BITS - 1);
    private static final int LIGHT_CACHE_MASK = LIGHT_CACHE_SIZE - 1;
    private final WorldRenderContext parent;
    private final long[] lightCacheKeys;
    private final int[] lightCache;
    private final BlockPos.Mutable scratchPos = new BlockPos.Mutable();
    private final Vector4f lightScratch = new Vector4f();
    private final MatrixStack lightMatrices;

    public BattleRenderContextImpl(final WorldRenderContext parent, final ClientBattleImpl battle) {
        this.parent = parent;
        lightCacheKeys = new long[LIGHT_CACHE_SIZE];
        lightCache = new int[LIGHT_CACHE_SIZE];
        Arrays.fill(lightCacheKeys, -1);
        lightMatrices = new MatrixStack();
        lightMatrices.translate(battle.worldX(0), battle.worldY(0), battle.worldZ(0));
    }

    @Override
    public WorldRenderContext parent() {
        return parent;
    }

    private int light(final int x, final int y, final int z) {
        final long key = BlockPos.asLong(x, y, z);
        final int index = (int) HashCommon.mix(key) & LIGHT_CACHE_MASK;
        if (lightCacheKeys[index] == key) {
            return lightCache[index];
        }
        final ClientWorld world = MinecraftClient.getInstance().world;
        scratchPos.set(x, y, z);
        final int light = LightmapTextureManager.pack(world.getLightLevel(LightType.BLOCK, scratchPos), world.getLightLevel(LightType.SKY, scratchPos));
        lightCacheKeys[index] = key;
        lightCache[index] = light;
        return light;
    }

    @Override
    public int light(final double x, final double y, final double z) {
        lightMatrices.peek().getPositionMatrix().transform(lightScratch.set((float) x, (float) y, (float) z, 1.0F));
        final int lx = MathHelper.floor(lightScratch.x);
        final int ly = MathHelper.floor(lightScratch.y);
        final int lz = MathHelper.floor(lightScratch.z);
        final int ux = MathHelper.ceil(lightScratch.x);
        final int uy = MathHelper.ceil(lightScratch.y);
        final int uz = MathHelper.ceil(lightScratch.z);
        return lerpLight(
                light(lx, ly, lz),
                light(lx, ly, uz),
                light(lx, uy, lz),
                light(lx, uy, uz),
                light(ux, ly, lz),
                light(ux, ly, uz),
                light(ux, uy, lz),
                light(ux, uy, uz),
                MathHelper.fractionalPart(x),
                MathHelper.fractionalPart(y),
                MathHelper.fractionalPart(z)
        );
    }

    @Override
    public MatrixStack lightMatrices() {
        return lightMatrices;
    }

    private static double mixLight(final int l, final int u, final double alpha, final LightType type) {
        final int lUnpacked = (type == LightType.BLOCK) ? LightmapTextureManager.getBlockLightCoordinates(l) : LightmapTextureManager.getSkyLightCoordinates(l);
        final int uUnpacked = (type == LightType.BLOCK) ? LightmapTextureManager.getBlockLightCoordinates(u) : LightmapTextureManager.getSkyLightCoordinates(u);
        return lUnpacked * (1 - alpha) + uUnpacked * alpha;
    }

    private static int lerpLight(final int lll, final int llu, final int lul, final int luu, final int ull, final int ulu, final int uul, final int uuu, final double x, final double y, final double z) {
        final double zBlock0 = mixLight(lll, llu, z, LightType.BLOCK);
        final double zBlock1 = mixLight(lul, luu, z, LightType.BLOCK);
        final double zBlock2 = mixLight(ull, ulu, z, LightType.BLOCK);
        final double zBlock3 = mixLight(uul, uuu, z, LightType.BLOCK);

        final double zSky0 = mixLight(lll, llu, z, LightType.SKY);
        final double zSky1 = mixLight(lul, luu, z, LightType.SKY);
        final double zSky2 = mixLight(ull, ulu, z, LightType.SKY);
        final double zSky3 = mixLight(uul, uuu, z, LightType.SKY);

        final double yBlock0 = MathHelper.lerp(y, zBlock0, zBlock1);
        final double yBlock1 = MathHelper.lerp(y, zBlock2, zBlock3);

        final double ySky0 = MathHelper.lerp(y, zSky0, zSky1);
        final double ySky1 = MathHelper.lerp(y, zSky2, zSky3);

        final double xBlock = MathHelper.lerp(x, yBlock0, yBlock1);
        final double xSky = MathHelper.lerp(x, ySky0, ySky1);

        return LightmapTextureManager.pack(MathHelper.clamp(Math.round((float) xBlock), 0, 15), MathHelper.clamp(Math.round((float) xSky), 0, 15));
    }
}
