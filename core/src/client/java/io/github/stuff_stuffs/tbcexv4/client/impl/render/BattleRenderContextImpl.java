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
import org.joml.Matrix4f;
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
    private final Vector4f lightScratch0 = new Vector4f();
    private final Vector4f lightScratch1 = new Vector4f();
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
        lightMatrices.peek().getPositionMatrix().transform(lightScratch0.set((float) x, (float) y, (float) z, 1.0F));
        final int lx;
        final int ly;
        final int lz;
        final int ux;
        final int uy;
        final int uz;
        final double rx;
        final double ry;
        final double rz;
        if (MathHelper.fractionalPart(lightScratch0.x) < 0.5) {
            lx = MathHelper.floor(lightScratch0.x) - 1;
            ux = lx + 1;
            rx = MathHelper.fractionalPart(lightScratch0.x + 0.5);
        } else {
            lx = MathHelper.floor(lightScratch0.x);
            ux = lx + 1;
            rx = MathHelper.fractionalPart(lightScratch0.x - 0.5);
        }

        if (MathHelper.fractionalPart(lightScratch0.y) < 0.5) {
            ly = MathHelper.floor(lightScratch0.y) - 1;
            uy = ly + 1;
            ry = MathHelper.fractionalPart(lightScratch0.y + 0.5);
        } else {
            ly = MathHelper.floor(lightScratch0.y);
            uy = ly + 1;
            ry = MathHelper.fractionalPart(lightScratch0.y - 0.5);
        }

        if (MathHelper.fractionalPart(lightScratch0.z) < 0.5) {
            lz = MathHelper.floor(lightScratch0.z) - 1;
            uz = lz + 1;
            rz = MathHelper.fractionalPart(lightScratch0.z + 0.5);
        } else {
            lz = MathHelper.floor(lightScratch0.z);
            uz = lz + 1;
            rz = MathHelper.fractionalPart(lightScratch0.z - 0.5);
        }
        return lerpLight(
                light(lx, ly, lz),
                light(lx, ly, uz),
                light(lx, uy, lz),
                light(lx, uy, uz),
                light(ux, ly, lz),
                light(ux, ly, uz),
                light(ux, uy, lz),
                light(ux, uy, uz),
                rx,
                ry,
                rz
        );
    }

    @Override
    public void light(final double x0, final double y0, final double z0, final double x1, final double y1, final double z1, final LightResult result) {
        final Matrix4f pMat = lightMatrices.peek().getPositionMatrix();
        pMat.transform(lightScratch0.set((float) x0, (float) y0, (float) z0, 1.0F));
        pMat.transform(lightScratch1.set((float) x1, (float) y1, (float) z1, 1.0F));
        final boolean collapseX = Math.abs(lightScratch0.x - lightScratch1.x) <= 1.0;
        final boolean collapseY = Math.abs(lightScratch0.y - lightScratch1.y) <= 1.0;
        final boolean collapseZ = Math.abs(lightScratch0.z - lightScratch1.z) <= 1.0;
        if (collapseX & collapseY & collapseZ) {
            final double x = (x0 + x1) * 0.5;
            final double y = (y0 + y1) * 0.5;
            final double z = (z0 + z1) * 0.5;
            final int l = light(x, y, z);
            result.l0 = l;
            result.l1 = l;
            result.l2 = l;
            result.l3 = l;
            result.l4 = l;
            result.l5 = l;
            result.l6 = l;
            result.l7 = l;
        } else if (collapseX & collapseY) {
            final double x = (x0 + x1) * 0.5;
            final double y = (y0 + y1) * 0.5;
            final int lowerZ = light(x, y, z0);
            final int upperZ = light(x, y, z1);
            result.l0 = lowerZ;
            result.l1 = lowerZ;
            result.l2 = lowerZ;
            result.l3 = lowerZ;
            result.l4 = upperZ;
            result.l5 = upperZ;
            result.l6 = upperZ;
            result.l7 = upperZ;
        } else if (collapseX & collapseZ) {
            final double x = (x0 + x1) * 0.5;
            final double z = (z0 + z1) * 0.5;
            final int lowerY = light(x, y0, z);
            final int upperY = light(x, y1, z);
            result.l0 = lowerY;
            result.l1 = lowerY;
            result.l2 = upperY;
            result.l3 = upperY;
            result.l4 = lowerY;
            result.l5 = lowerY;
            result.l6 = upperY;
            result.l7 = upperY;
        } else if (collapseY & collapseZ) {
            final double y = (y0 + y1) * 0.5;
            final double z = (z0 + z1) * 0.5;
            final int lowerX = light(x0, y, z);
            final int upperX = light(x1, y, z);
            result.l0 = lowerX;
            result.l1 = upperX;
            result.l2 = upperX;
            result.l3 = lowerX;
            result.l4 = lowerX;
            result.l5 = upperX;
            result.l6 = upperX;
            result.l7 = lowerX;
        } else if (collapseX) {
            final double x = (x0 + x1) * 0.5;
            final int l00 = light(x, y0, z0);
            final int l10 = light(x, y1, z0);
            final int l01 = light(x, y0, z1);
            final int l11 = light(x, y1, z1);
            result.l0 = l00;
            result.l1 = l00;
            result.l2 = l10;
            result.l3 = l10;
            result.l4 = l01;
            result.l5 = l01;
            result.l6 = l11;
            result.l7 = l11;
        } else if (collapseY) {
            final double y = (y0 + y1) * 0.5;
            final int l00 = light(x0, y, z0);
            final int l10 = light(x1, y, z0);
            final int l01 = light(x0, y, z1);
            final int l11 = light(x1, y, z1);
            result.l0 = l00;
            result.l1 = l10;
            result.l2 = l10;
            result.l3 = l00;
            result.l4 = l01;
            result.l5 = l11;
            result.l6 = l11;
            result.l7 = l01;
        } else if (collapseZ) {
            final double z = (z0 + z1) * 0.5;
            final int l00 = light(x0, y0, z);
            final int l10 = light(x1, y0, z);
            final int l01 = light(x0, y1, z);
            final int l11 = light(x1, y1, z);
            result.l0 = l00;
            result.l1 = l10;
            result.l2 = l11;
            result.l3 = l01;
            result.l4 = l00;
            result.l5 = l10;
            result.l6 = l11;
            result.l7 = l01;
        } else {
            result.l0 = light(x0, y0, z0);
            result.l1 = light(x1, y0, z0);
            result.l2 = light(x1, y1, z0);
            result.l3 = light(x0, y1, z0);
            result.l4 = light(x0, y0, z1);
            result.l5 = light(x1, y0, z1);
            result.l6 = light(x1, y1, z1);
            result.l7 = light(x0, y1, z1);
        }
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
