package io.github.stuff_stuffs.tbcexv4.client.internal;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class BattleDebugRendererRegistry {
    private static final Identifier HEALTH_BACKGROUND = new Identifier("boss_bar/notched_20_background");
    private static final Identifier HEALTH_FOREGROUND = new Identifier("boss_bar/white_progress");

    public static final class RenderLayerHolder extends RenderLayer {
        public static final Function<Float, RenderLayer> DEBUG_LINES = Util.memoize(lineWidth -> RenderLayer.of(
                "debug_line",
                VertexFormats.POSITION_COLOR,
                VertexFormat.DrawMode.DEBUG_LINES,
                1536,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(COLOR_PROGRAM)
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(lineWidth)))
                        .transparency(NO_TRANSPARENCY)
                        .cull(DISABLE_CULLING)
                        .build(false)
        ));
        public static final RenderLayer GUI = RenderLayer.of("gui", VertexFormats.POSITION_COLOR_TEXTURE, VertexFormat.DrawMode.QUADS, 4096, false, true, RenderLayer.MultiPhaseParameters.builder().program(POSITION_COLOR_TEXTURE_PROGRAM).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).cull(RenderPhase.DISABLE_CULLING).texture(new Texture(new Identifier("textures/atlas/gui.png"), false, false)).depthTest(RenderPhase.LEQUAL_DEPTH_TEST).build(false));

        public RenderLayerHolder(final String name, final VertexFormat vertexFormat, final VertexFormat.DrawMode drawMode, final int expectedBufferSize, final boolean hasCrumbling, final boolean translucent, final Runnable startAction, final Runnable endAction) {
            super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
        }
    }

    private static final Map<String, BattleDebugRenderer> REGISTRY = new HashMap<>();
    private static final Set<String> ENABLED = new ObjectOpenHashSet<>();

    public static void register(final String id, final BattleDebugRenderer renderer) {
        if (REGISTRY.putIfAbsent(id, renderer) != null) {
            throw new RuntimeException("Duplicate battle debug renderer ids!");
        }
    }

    public static BattleDebugRenderer get(final String id) {
        return REGISTRY.get(id);
    }

    public static Set<String> ids() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    public static Set<String> enabled() {
        return Collections.unmodifiableSet(ENABLED);
    }

    public static void enable(final String id) {
        if (!REGISTRY.containsKey(id)) {
            throw new RuntimeException("Unknown debug renderer!");
        }
        ENABLED.add(id);
    }

    public static void disable(final String id) {
        if (!REGISTRY.containsKey(id)) {
            throw new RuntimeException("Unknown debug renderer!");
        }
        ENABLED.remove(id);
    }

    public static void init() {
        register("bounds", (context, battle) -> {
            final VertexConsumer buffer = context.consumers().getBuffer(RenderLayerHolder.DEBUG_LINES.apply(2.0F));
            final Camera camera = context.camera();
            final float startX = (float) (battle.worldX(0) - camera.getPos().x);
            final float startY = (float) (battle.worldY(0) - camera.getPos().y);
            final float startZ = (float) (battle.worldZ(0) - camera.getPos().z);
            final float endX = (float) (battle.worldX(battle.xSize()) - camera.getPos().x);
            final float endY = (float) (battle.worldY(battle.ySize()) - camera.getPos().y);
            final float endZ = (float) (battle.worldZ(battle.ySize()) - camera.getPos().z);
            final Matrix4f pMat = context.matrixStack().peek().getPositionMatrix();
            for (float y = startY; y <= endY + 0.00001; y += 8) {
                buffer.vertex(pMat, startX, y, startZ).color(0xFF0000FF).normal(1, 0, 0).next();
                buffer.vertex(pMat, endX, y, startZ).color(0xFF0000FF).normal(1, 0, 0).next();

                buffer.vertex(pMat, startX, y, endZ).color(0xFF0000FF).normal(1, 0, 0).next();
                buffer.vertex(pMat, endX, y, endZ).color(0xFF0000FF).normal(1, 0, 0).next();

                buffer.vertex(pMat, startX, y, startZ).color(0xFFFF0000).normal(1, 0, 0).next();
                buffer.vertex(pMat, startX, y, endZ).color(0xFFFF0000).normal(1, 0, 0).next();

                buffer.vertex(pMat, endX, y, startZ).color(0xFFFF0000).normal(1, 0, 0).next();
                buffer.vertex(pMat, endX, y, endZ).color(0xFFFF0000).normal(1, 0, 0).next();
            }
            for (float x = startX; x < endX + 0.00001; x += 8) {
                buffer.vertex(pMat, x, startY, startZ).color(0xFFFF00FF).normal(0, 0, 0).next();
                buffer.vertex(pMat, x, endY, startZ).color(0xFFFF00FF).normal(0, 0, 0).next();

                buffer.vertex(pMat, x, startY, endZ).color(0xFFFF00FF).normal(0, 0, 1).next();
                buffer.vertex(pMat, x, endY, endZ).color(0xFFFF00FF).normal(0, 0, 1).next();
            }
            for (float z = startZ; z < endZ + 0.00001; z += 8) {
                buffer.vertex(pMat, startX, startY, z).color(0xFFFF00FF).normal(0, 0, 1).next();
                buffer.vertex(pMat, startX, endY, z).color(0xFFFF00FF).normal(0, 0, 1).next();

                buffer.vertex(pMat, endX, startY, z).color(0xFFFF00FF).normal(0, 0, 1).next();
                buffer.vertex(pMat, endX, endY, z).color(0xFFFF00FF).normal(0, 0, 1).next();
            }
        });
        register("participant_health", (context, battle) -> {
            final Vec3d cameraPos = context.camera().getPos();
            final MatrixStack matrices = context.matrixStack();
            matrices.push();
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            final VertexConsumerProvider consumers = context.consumers();
            for (final BattleParticipantHandle handle : battle.state().participants()) {
                final BattleParticipant participant = battle.state().participant(handle);
                final float healthPercent = (float) (participant.health() / participant.stats().get(Tbcexv4Registries.Stats.MAX_HEALTH));
                if (!Float.isFinite(healthPercent)) {
                    continue;
                }
                matrices.push();
                final double x = battle.worldX(participant.pos().x()) + 0.5;
                final double y = battle.worldY(participant.pos().y()) + participant.bounds().height() + 1;
                final double z = battle.worldZ(participant.pos().z()) + 0.5;
                matrices.translate(x, y, z);
                matrices.multiply(context.camera().getRotation());
                final Matrix4f pMat = matrices.peek().getPositionMatrix();
                final int i = participant.team().id().hashCode() & 0x7FFFFFFF;
                final int rgb = MathHelper.hsvToRgb(i / (float) Integer.MAX_VALUE, 1.0F, 1.5F);
                final float red = ColorHelper.Argb.getRed(rgb) / 255.0F;
                final float green = ColorHelper.Argb.getGreen(rgb) / 255.0F;
                final float blue = ColorHelper.Argb.getBlue(rgb) / 255.0F;
                final VertexConsumer buffer = consumers.getBuffer(RenderLayerHolder.GUI);
                {
                    final Sprite sprite = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(new Identifier("boss_bar/white_background"));
                    buffer.vertex(pMat, -0.75F, -0.075F, 0.0F).color(red, green, blue, 1.0F).texture(sprite.getMinU(), sprite.getMinV()).next();
                    buffer.vertex(pMat, 0.75F, -0.075F, 0.0F).color(red, green, blue, 1.0F).texture(sprite.getMaxU(), sprite.getMinV()).next();
                    buffer.vertex(pMat, 0.75F, 0.075F, 0.0F).color(red, green, blue, 1.0F).texture(sprite.getMaxU(), sprite.getMaxV()).next();
                    buffer.vertex(pMat, -0.75F, 0.075F, 0.0F).color(red, green, blue, 1.0F).texture(sprite.getMinU(), sprite.getMaxV()).next();
                }
                {
                    final Sprite sprite = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(new Identifier("boss_bar/white_progress"));
                    final float maxU = (sprite.getMaxU() - sprite.getMinU()) * healthPercent + sprite.getMinU();
                    buffer.vertex(pMat, -0.75F, -0.075F, -0.001F).color(red, green, blue, 1.0F).texture(maxU, sprite.getMinV()).next();
                    buffer.vertex(pMat, 0.75F * healthPercent, -0.075F, -0.001F).color(red, green, blue, 1.0F).texture(sprite.getMinU(), sprite.getMinV()).next();
                    buffer.vertex(pMat, 0.75F * healthPercent, 0.075F, -0.001F).color(red, green, blue, 1.0F).texture(sprite.getMinU(), sprite.getMaxV()).next();
                    buffer.vertex(pMat, -0.75F, 0.075F, -0.001F).color(red, green, blue, 1.0F).texture(maxU, sprite.getMaxV()).next();
                }

                matrices.pop();
            }
            matrices.pop();
        });
        register("participant_bounds", (context, battle) -> {
            final VertexConsumer buffer = context.consumers().getBuffer(RenderLayerHolder.DEBUG_LINES.apply(1.0F));
            final Vec3d cameraPos = context.camera().getPos();
            final MatrixStack matrices = context.matrixStack();

            for (final BattleParticipantHandle handle : battle.state().participants()) {
                final BattleParticipant participant = battle.state().participant(handle);
                final int i = participant.team().id().hashCode() & 0x7FFFFFFF;
                final int rgb = MathHelper.hsvToRgb(i / (float) Integer.MAX_VALUE, 1.0F, 1.5F);
                final BattleParticipantBounds bounds = participant.bounds();
                final Box box = bounds.asBox(
                        battle.worldX(participant.pos().x()) - cameraPos.x,
                        battle.worldY(participant.pos().y()) - cameraPos.y,
                        battle.worldZ(participant.pos().z()) - cameraPos.z
                );
                final float red = ColorHelper.Argb.getRed(rgb) / 255.0F;
                final float green = ColorHelper.Argb.getGreen(rgb) / 255.0F;
                final float blue = ColorHelper.Argb.getBlue(rgb) / 255.0F;
                WorldRenderer.drawBox(matrices, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, 1);
            }
            for (final BattleParticipantHandle handle : battle.state().participants()) {
                final BattleParticipant participant = battle.state().participant(handle);
                final BattleParticipantBounds bounds = participant.bounds();
                final Box box = bounds.asBox(
                        battle.worldX(participant.pos().x()) - cameraPos.x,
                        battle.worldY(participant.pos().y()) - cameraPos.y,
                        battle.worldZ(participant.pos().z()) - cameraPos.z
                );
                matrices.push();
                matrices.translate((box.minX + box.maxX) * 0.5, box.maxY + 0.5, (box.minZ + box.maxZ) * 0.5);
                matrices.multiply(context.camera().getRotation());
                matrices.scale(-0.025F, -0.025F, 0.025F);
                final Text text = Text.of(handle.id() + "");
                final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                final float h = (float) (-textRenderer.getWidth(text) / 2);
                textRenderer.draw(text, h, 0, Colors.WHITE, false, matrices.peek().getPositionMatrix(), context.consumers(), TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.pack(15, 15));
                matrices.pop();
            }
        });
    }

    private BattleDebugRendererRegistry() {
    }

    public static final class DebugRendererArgumentType implements ArgumentType<String> {
        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
            return CommandSource.suggestMatching(BattleDebugRendererRegistry.ids(), builder);
        }

        @Override
        public Collection<String> getExamples() {
            return BattleDebugRendererRegistry.ids();
        }

        @Override
        public String parse(final StringReader reader) {
            return reader.readUnquotedString();
        }
    }
}
